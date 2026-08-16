package yuboobo.equipment.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import yuboobo.equipment.EquipmentPlus;

/**
 * Server-side slow-motion core plus the client-side render factor lookup.
 *
 * <p>While at least one player has an active {@link TimeControlSource}, every
 * non-activating entity inside the source's range is slowed: its tick is executed
 * only once every {@code 1 / factor} ticks (phase-offset by entity id), and its
 * velocity is scaled by the factor on the skipped ticks.
 *
 * <p>Sources that {@link TimeControlSource#slowsPlayer() slow their wearer} reduce
 * only the wearer's movement speed via an attribute modifier (their tick is never
 * throttled, so controls and camera stay responsive). Activating players are always
 * exempt from tick throttling.
 *
 * <p>To extend to other accessories (e.g. a nervous system) simply register
 * another {@link TimeControlSource} in the item's equip hook.
 */
public final class TimeSlowManager {

	/** Movement speed modifier id applied to players slowed by their own source. */
	public static final Identifier WEARER_SLOW_MODIFIER = EquipmentPlus.id("wearer_slow");

	/** Active players mapped to their aggregated (slowest) world factor. */
	private static final Map<ServerPlayer, Double> ACTIVE_PLAYERS = new ConcurrentHashMap<>();
	/** Per-player registered sources. */
	private static final Map<ServerPlayer, List<TimeControlSource>> SOURCES =
		new ConcurrentHashMap<>();
	/** Players whose OS skill panel is currently open. */
	private static final Map<ServerPlayer, Boolean> PANEL_OPEN = new ConcurrentHashMap<>();

	/** The source registered by the operating system while worn. */
	public static final TimeControlSource OS_SOURCE = new TimeControlSource() {

		@Override
		public double speedFactor() {
			return TimeSlowConfig.speedFactor();
		}

		@Override
		public double range() {
			return TimeSlowConfig.slowRange();
		}

		@Override
		public boolean slowsPlayer() {
			return true;
		}

		@Override
		public boolean isActive(ServerPlayer player) {
			return PANEL_OPEN.getOrDefault(player, false);
		}
	};

	/** The source registered by the nervous system while worn (always active). */
	public static final TimeControlSource NERVOUS_SOURCE = new TimeControlSource() {

		@Override
		public double speedFactor() {
			return TimeSlowConfig.speedFactor();
		}

		@Override
		public double range() {
			return TimeSlowConfig.slowRange();
		}

		@Override
		public boolean slowsPlayer() {
			return false;
		}

		@Override
		public boolean isActive(ServerPlayer player) {
			return true;
		}
	};

	private TimeSlowManager() {
	}

	public static void registerSource(ServerPlayer player, TimeControlSource source) {
		SOURCES.computeIfAbsent(player, ignored -> new ArrayList<>()).add(source);
	}

	public static void unregisterSource(ServerPlayer player, TimeControlSource source) {
		List<TimeControlSource> list = SOURCES.get(player);

		if (list != null) {
			list.remove(source);

			if (list.isEmpty()) {
				SOURCES.remove(player);
				setPanelOpen(player, false);
			}
		}
	}

	public static void setPanelOpen(ServerPlayer player, boolean open) {
		PANEL_OPEN.put(player, open);
	}

	/**
	 * Called on every server tick; recomputes which players are currently slowing
	 * the world and maintains the wearer movement-speed modifiers.
	 */
	public static void tick() {
		ACTIVE_PLAYERS.clear();

		for (Map.Entry<ServerPlayer, List<TimeControlSource>> entry : SOURCES.entrySet()) {
			ServerPlayer player = entry.getKey();

			if (player.isRemoved() || player.level() == null) {
				SOURCES.remove(player);
				PANEL_OPEN.remove(player);
				removeWearerModifier(player);
				continue;
			}
			double slowest = 1.0D;
			double wearerFactor = 1.0D;
			boolean anyActive = false;

			for (TimeControlSource source : entry.getValue()) {

				if (source.isActive(player)) {
					anyActive = true;
					slowest = Math.min(slowest, source.speedFactor());

					if (source.slowsPlayer()) {
						wearerFactor = Math.min(wearerFactor, source.speedFactor());
					}
				}
			}

			if (anyActive) {
				ACTIVE_PLAYERS.put(player, slowest);
			}
			applyWearerModifier(player, wearerFactor);
		}
	}

	/**
	 * Returns the slow factor that applies to the given entity this tick (server
	 * side). {@code 1.0} means no slow. Activating players are never throttled.
	 */
	public static double getFactorFor(Entity entity) {

		if (ACTIVE_PLAYERS.isEmpty()) {
			return 1.0D;
		}

		if (entity instanceof ServerPlayer serverPlayer && ACTIVE_PLAYERS.containsKey(serverPlayer)) {
			return 1.0D;
		}
		double slowest = 1.0D;

		for (Map.Entry<ServerPlayer, Double> entry : ACTIVE_PLAYERS.entrySet()) {
			ServerPlayer player = entry.getKey();

			if (!player.isAlive() || player.level() != entity.level()) {
				continue;
			}
			double range = rangeFor(player);

			if (range > 0 && entity.distanceToSqr(player) <= range * range) {
				slowest = Math.min(slowest, entry.getValue());
			}
		}
		return slowest;
	}

	private static double rangeFor(ServerPlayer player) {
		double range = 0;

		for (TimeControlSource source : SOURCES.getOrDefault(player, List.of())) {

			if (source.isActive(player)) {
				range = Math.max(range, source.range());
			}
		}
		return range;
	}

	/**
	 * Whether this living entity's tick should be throttled (mobs slow down their
	 * behaviour and attacks). Dying entities are exempt so death animations play
	 * out normally.
	 */
	public static boolean isThrottled(Entity entity) {
		double factor = getFactorFor(entity);

		if (factor >= 1.0D || !(entity instanceof LivingEntity)) {
			return false;
		}
		return !((LivingEntity) entity).isDeadOrDying();
	}

	/**
	 * Whether the throttled entity's tick should be skipped this tick
	 * (phase-offset by entity id).
	 */
	public static boolean shouldSkipTick(Entity entity) {
		double factor = getFactorFor(entity);
		int interval = Math.max(2, (int) Math.round(1.0D / factor));

		return (entity.level().getGameTime() + entity.getId()) % interval != 0;
	}

	/**
	 * Server-side slow factor for physics entities (projectiles, items, vehicles):
	 * their tick runs every tick but their movement is scaled to the factor by the
	 * caller, giving a smooth slow-motion without corrupting their velocity.
	 */
	public static double getSlowFactor(Entity entity) {
		return getFactorFor(entity);
	}

	private static void applyWearerModifier(ServerPlayer player, double factor) {
		AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

		if (attribute == null) {
			return;
		}

		if (factor < 1.0D) {
			if (attribute.getModifier(WEARER_SLOW_MODIFIER) == null) {
				attribute.addTransientModifier(new AttributeModifier(WEARER_SLOW_MODIFIER,
					factor - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else {
			attribute.removeModifier(WEARER_SLOW_MODIFIER);
		}
	}

	private static void removeWearerModifier(ServerPlayer player) {
		AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

		if (attribute != null) {
			attribute.removeModifier(WEARER_SLOW_MODIFIER);
		}
	}

	public static void removePlayer(ServerPlayer player) {
		SOURCES.remove(player);
		PANEL_OPEN.remove(player);
		ACTIVE_PLAYERS.remove(player);
		removeWearerModifier(player);
		EquipmentPlus.LOGGER.debug("TimeSlowManager removed player {}", player.getScoreboardName());
	}

	public static void onServerStart(net.minecraft.server.MinecraftServer server) {
		SOURCES.clear();
		PANEL_OPEN.clear();
		ACTIVE_PLAYERS.clear();
	}

	public static void onServerStop() {
		SOURCES.clear();
		PANEL_OPEN.clear();
		ACTIVE_PLAYERS.clear();
	}
}

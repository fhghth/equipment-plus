package yuboobo.equipment.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import yuboobo.accessories.api.AccessoriesAPI;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.item.CharmItem;

public class SpeedBoostManager {

	public static final Identifier SPEED_MODIFIER_ID = EquipmentPlus.id("speed_boost");
	public static final int BOOST_DURATION_TICKS = 200;
	public static final int COOLDOWN_TICKS = 300;
	private static final double SPEED_BOOST = 0.35D;

	private static final Map<UUID, State> STATES = new HashMap<>();

	private record State(long boostUntilTick, long cooldownUntilTick) {
	}

	private SpeedBoostManager() {
	}

	public static boolean tryActivate(ServerPlayer player) {
		long gameTime = player.level().getGameTime();
		UUID uuid = player.getUUID();
		State state = STATES.get(uuid);

		if (state != null && gameTime < state.cooldownUntilTick) {
			return false;
		}

		if (AccessoriesAPI.getCuriosInventory(player)
			.flatMap(inventory -> inventory.findFirstCurio(EquipmentPlus.CHARM))
			.isEmpty()) {
			return false;
		}

		AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

		if (attribute != null) {
			attribute.removeModifier(SPEED_MODIFIER_ID);
			attribute.addTransientModifier(
				new AttributeModifier(SPEED_MODIFIER_ID, SPEED_BOOST,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}

		STATES.put(uuid, new State(gameTime + BOOST_DURATION_TICKS,
			gameTime + COOLDOWN_TICKS));
		return true;
	}

	public static void tick(ServerPlayer player) {
		UUID uuid = player.getUUID();
		State state = STATES.get(uuid);

		if (state == null) {
			return;
		}

		if (player.level().getGameTime() >= state.boostUntilTick) {
			AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

			if (attribute != null) {
				attribute.removeModifier(SPEED_MODIFIER_ID);
			}
		}
	}

	public static int getCooldownRemainingTicks(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null) {
			return 0;
		}
		return (int) Math.max(0L, state.cooldownUntilTick - player.level().getGameTime());
	}

	public static void remove(UUID uuid) {
		STATES.remove(uuid);
	}
}

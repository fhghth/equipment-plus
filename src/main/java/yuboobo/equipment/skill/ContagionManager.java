package yuboobo.equipment.skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Handles the Contagion skill: infects a target with poison and spreads the poison to
 * nearby non-player living entities every {@link #SPREAD_INTERVAL} ticks while the
 * effect lasts. All infected entities share the same expiry, so the chain dies out
 * together.
 */
public class ContagionManager {

	public static final int POISON_TICKS = 100;
	private static final int SPREAD_INTERVAL = 20;
	private static final double SPREAD_RADIUS = 4.0D;
	private static final Map<UUID, Long> infected = new HashMap<>();

	private ContagionManager() {
	}

	public static void infect(ServerLevel level, LivingEntity target) {
		long expiry = level.getGameTime() + POISON_TICKS;
		infected.put(target.getUUID(), expiry);
		target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
	}

	public static void tick(ServerLevel level) {
		long gameTime = level.getGameTime();

		infected.entrySet().removeIf(entry -> {
			Entity entity = level.getEntity(entry.getKey());
			return entry.getValue() <= gameTime
				|| entity == null
				|| !entity.isAlive();
		});

		if (gameTime % SPREAD_INTERVAL != 0 || infected.isEmpty()) {
			return;
		}

		for (Map.Entry<UUID, Long> entry : new ArrayList<>(infected.entrySet())) {
			Entity entity = level.getEntity(entry.getKey());

			if (entity instanceof LivingEntity source && source.isAlive()) {
				spread(level, source, entry.getValue());
			}
		}
	}

	private static void spread(ServerLevel level, LivingEntity source, long expiry) {
		long remaining = expiry - level.getGameTime();

		if (remaining <= 0) {
			return;
		}

		List<Entity> candidates = level.getEntities(source,
			source.getBoundingBox().inflate(SPREAD_RADIUS),
			candidate -> candidate instanceof LivingEntity
				&& candidate != source
				&& !(candidate instanceof ServerPlayer)
				&& candidate.isAlive()
				&& !infected.containsKey(candidate.getUUID()));

		for (Entity candidate : candidates) {
			LivingEntity target = (LivingEntity) candidate;
			target.addEffect(new MobEffectInstance(MobEffects.POISON, (int) remaining, 0));
			infected.put(target.getUUID(), expiry);
		}
	}
}
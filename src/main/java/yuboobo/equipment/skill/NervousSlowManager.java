package yuboobo.equipment.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;

import yuboobo.equipment.item.NervousSystemItem;
import yuboobo.equipment.time.TimeSlowManager;

/**
 * Server-side manager for the nervous-system slow-motion ability: pressing the
 * nervous system key grants a short world slow (the wearer themselves is never
 * slowed) followed by a cooldown.
 */
public class NervousSlowManager {

	public static final int SLOW_DURATION_TICKS = 200; // 10 seconds
	public static final int COOLDOWN_TICKS = 300;      // 15 seconds

	private static final Map<UUID, State> STATES = new HashMap<>();

	private record State(long activeUntilTick, long cooldownUntilTick) {
	}

	private NervousSlowManager() {
	}

	public static boolean isEquipped(ServerPlayer player) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);
		return inventory != null
			&& inventory.findFirstCurio(
				stack -> stack.getItem() instanceof NervousSystemItem).isPresent();
	}

	/**
	 * Attempts to activate the nervous-system slow. Returns true when the effect
	 * was started; false when on cooldown or no Sandevistan is equipped.
	 */
	public static boolean tryActivate(ServerPlayer player) {
		long gameTime = player.level().getGameTime();
		UUID uuid = player.getUUID();
		State state = STATES.get(uuid);

		if (state != null && gameTime < state.cooldownUntilTick) {
			return false;
		}

		if (!isEquipped(player)) {
			return false;
		}

		TimeSlowManager.registerSource(player, TimeSlowManager.NERVOUS_SOURCE);
		STATES.put(uuid, new State(gameTime + SLOW_DURATION_TICKS,
			gameTime + COOLDOWN_TICKS));
		return true;
	}

	/**
	 * Called every tick while the nervous system is equipped; ends the slow when
	 * its duration has expired.
	 */
	public static void tick(ServerPlayer player) {
		UUID uuid = player.getUUID();
		State state = STATES.get(uuid);

		if (state == null) {
			return;
		}

		if (player.level().getGameTime() >= state.activeUntilTick) {
			TimeSlowManager.unregisterSource(player, TimeSlowManager.NERVOUS_SOURCE);
		}
	}

	public static int getCooldownRemainingTicks(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null) {
			return 0;
		}
		return (int) Math.max(0L, state.cooldownUntilTick - player.level().getGameTime());
	}

	public static void remove(ServerPlayer player) {
		STATES.remove(player.getUUID());
		TimeSlowManager.unregisterSource(player, TimeSlowManager.NERVOUS_SOURCE);
	}
}

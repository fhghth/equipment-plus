package yuboobo.equipment.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import yuboobo.equipment.time.TimeSlowConfig;

/**
 * Client-side knowledge of the slow-motion state. The OS panel state is already
 * known locally; the nervous-system slow is mirrored from the server's activation
 * confirmation and counted down locally.
 *
 * <p>Used to stop client-side entity ticks (e.g. arrows) while the world is
 * slowed, so their rendered position is driven purely by the server position
 * packets instead of a local full-speed prediction that gets corrected.
 */
public final class TimeSlowClient {

	private static final int NERVOUS_SLOW_TICKS = 200; // 10 seconds

	private static int nervousTicksLeft;

	private TimeSlowClient() {
	}

	/**
	 * Called when the server confirms a nervous-system activation.
	 */
	public static void activateNervousSlow() {
		nervousTicksLeft = NERVOUS_SLOW_TICKS;
	}

	/**
	 * Called every client tick to count the nervous-system slow down.
	 */
	public static void tick() {

		if (nervousTicksLeft > 0) {
			nervousTicksLeft--;
		}
	}

	/**
	 * Whether the world is currently slowed from this client's perspective.
	 */
	public static boolean isSlowActive() {
		return OSPanelManager.isOpen() || nervousTicksLeft > 0;
	}

	/**
	 * Whether the given entity is inside the local player's slow range.
	 */
	public static boolean inRange(Entity entity) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null || entity == player) {
			return false;
		}
		double range = TimeSlowConfig.slowRange();

		return range > 0 && entity.distanceToSqr(player) <= range * range;
	}
}

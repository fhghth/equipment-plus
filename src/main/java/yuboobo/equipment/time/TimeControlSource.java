package yuboobo.equipment.time;

import net.minecraft.server.level.ServerPlayer;

/**
 * A source of world time-slow for a player. Each equipped accessory may register
 * one or more sources; the slow-motion manager aggregates all active sources of a
 * player and applies the slowest factor.
 *
 * <p>The operating system registers a source that is active while its skill panel
 * is open and also slows the wearer (movement speed only, camera stays responsive).
 * A future nervous system can register a source with {@link #slowsPlayer()} false,
 * giving the wearer a "Sandevistan" style speed advantage over the slowed world.
 */
public interface TimeControlSource {

	/**
	 * The resulting world speed factor while this source is active.
	 * {@code 0.02} means the world moves at 2% speed (98% slow).
	 */
	double speedFactor();

	/**
	 * The radius (in blocks) around the player within which entities are slowed.
	 */
	double range();

	/**
	 * Whether this source also slows the activating player themselves.
	 * When true, only the wearer's movement speed is reduced (their tick is never
	 * throttled, so controls and camera stay responsive).
	 */
	boolean slowsPlayer();

	/**
	 * Whether this source is currently active for the given player.
	 */
	boolean isActive(ServerPlayer player);
}

package yuboobo.equipment.skill;

import java.util.function.ToIntFunction;

import net.minecraft.server.level.ServerPlayer;

/**
 * The ability a nervous-type accessory grants when the nervous system key (C) is
 * pressed. Variants declare their own activation and cooldown logic and are bound to
 * an item class through {@link NervousAbilityRegistry}.
 */
public interface NervousAbility {

	/**
	 * Variant id sent to the client so it can show the matching feedback message.
	 */
	int variant();

	/**
	 * Attempts to activate the ability. Returns true when the effect was started;
	 * false when on cooldown or the accessory is not equipped.
	 */
	boolean tryActivate(ServerPlayer player);

	int getCooldownRemainingTicks(ServerPlayer player);

	/**
	 * Builds an ability from the two handling methods, e.g.
	 * {@code NervousAbility.of(VARIANT_BERSERK, BerserkManager::tryActivate,
	 * BerserkManager::getCooldownRemainingTicks)}.
	 */
	static NervousAbility of(int variant, Activator activator,
							 ToIntFunction<ServerPlayer> cooldownTicks) {
		return new NervousAbility() {

			@Override
			public int variant() {
				return variant;
			}

			@Override
			public boolean tryActivate(ServerPlayer player) {
				return activator.tryActivate(player);
			}

			@Override
			public int getCooldownRemainingTicks(ServerPlayer player) {
				return cooldownTicks.applyAsInt(player);
			}
		};
	}

	@FunctionalInterface
	interface Activator {

		boolean tryActivate(ServerPlayer player);
	}
}
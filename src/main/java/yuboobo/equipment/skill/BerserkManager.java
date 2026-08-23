package yuboobo.equipment.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.item.BerserkItem;

/**
 * Server-side manager for the Berserk nervous-system ability: pressing the nervous
 * system key grants health regeneration and a +50% attack damage boost, followed by
 * a cooldown.
 */
public class BerserkManager {

	public static final int DURATION_TICKS = 240;   // 12 seconds
	public static final int COOLDOWN_TICKS = 340;   // 17 seconds
	public static final double ATTACK_MULTIPLIER = 0.5D; // +50% attack damage
	public static final Identifier ACTIVE_MODIFIER_ID = EquipmentPlus.id("berserk_active");

	private static final Map<UUID, State> STATES = new HashMap<>();

	private record State(long activeUntilTick, long cooldownUntilTick) {
	}

	private BerserkManager() {
	}

	public static boolean isEquipped(ServerPlayer player) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);
		return inventory != null
			&& inventory.findFirstCurio(
				stack -> stack.getItem() instanceof BerserkItem).isPresent();
	}

	/**
	 * Attempts to activate the Berserk ability. Returns true when the effect was
	 * started; false when on cooldown or no Berserk is equipped.
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

		AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);

		if (attribute != null && attribute.getModifier(ACTIVE_MODIFIER_ID) == null) {
			attribute.addTransientModifier(
				new AttributeModifier(ACTIVE_MODIFIER_ID, ATTACK_MULTIPLIER,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}

		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0));
		STATES.put(uuid, new State(gameTime + DURATION_TICKS, gameTime + COOLDOWN_TICKS));
		return true;
	}

	/**
	 * Called every tick while Berserk is equipped; ends the attack boost when its
	 * duration has expired.
	 */
	public static void tick(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null) {
			return;
		}

		if (player.level().getGameTime() >= state.activeUntilTick) {
			endActiveModifier(player);
		}
	}

	private static void endActiveModifier(ServerPlayer player) {
		AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);

		if (attribute != null) {
			attribute.removeModifier(ACTIVE_MODIFIER_ID);
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
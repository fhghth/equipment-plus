package yuboobo.equipment.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.SlotResult;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;

import yuboobo.equipment.item.NervousItem;

/**
 * Maps nervous-type accessories to their abilities. When the nervous system key is
 * pressed the worn nervous item is looked up and its registered ability executes,
 * so a new variant only needs its own item class and one registration here.
 */
public final class NervousAbilityRegistry {

	private static final Map<Class<? extends NervousItem>, NervousAbility> ABILITIES =
		new LinkedHashMap<>();

	public record Result(NervousAbility ability, boolean activated, int cooldownTicks) {
	}

	private NervousAbilityRegistry() {
	}

	public static void register(Class<? extends NervousItem> itemClass, NervousAbility ability) {
		ABILITIES.put(itemClass, ability);
	}

	/**
	 * Resolves the ability for the nervous accessory currently worn by the player and
	 * executes it. Returns null when no nervous accessory is worn or no ability is
	 * registered for it.
	 */
	public static Result activate(ServerPlayer player) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);

		if (inventory == null) {
			return null;
		}

		Optional<SlotResult> worn = inventory.findFirstCurio(
			stack -> stack.getItem() instanceof NervousItem);

		if (worn.isEmpty()) {
			return null;
		}

		NervousAbility ability = ABILITIES.get(worn.get().stack().getItem().getClass());

		if (ability == null) {
			return null;
		}

		boolean activated = ability.tryActivate(player);
		return new Result(ability, activated, ability.getCooldownRemainingTicks(player));
	}
}
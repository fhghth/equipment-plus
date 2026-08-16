package yuboobo.equipment.item;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;
import yuboobo.accessories.api.type.capability.ICurioItem;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.time.TimeSlowManager;

/**
 * Wearable operating system. While equipped, grants a fixed number of {@code os_skill}
 * slots (two for the basic model, more for advanced ones) via a permanent slot-size
 * modifier, so the skill slots only exist while this accessory is worn.
 */
public class OperatingSystemItem extends Item implements ICurioItem {

	public static final String OS_SKILL_SLOT = "os_skill";
	public static final Identifier OS_SKILL_SLOTS_MODIFIER =
		EquipmentPlus.id("os_skill_slots");

	private final int skillSlots;

	public OperatingSystemItem(Properties properties, int skillSlots) {
		super(properties);
		this.skillSlots = skillSlots;
	}

	/**
	 * The number of {@code os_skill} slots this operating system grants while equipped.
	 */
	public int getSkillSlotCount() {
		return this.skillSlots;
	}

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(OperatingSystemItem.this);
			}

			@Override
			public boolean canEquipFromUse(SlotContext slotContext) {
				return true;
			}

			@Override
			public void onEquip(SlotContext slotContext, ItemStack prevStack) {
				AccessoriesAPI.getCuriosInventory(slotContext.entity())
					.ifPresent(inventory -> {
						inventory.addPermanentSlotModifier(
							OS_SKILL_SLOT, OS_SKILL_SLOTS_MODIFIER, getSkillSlotCount(),
							AttributeModifier.Operation.ADD_VALUE);
					});

				if (slotContext.entity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					TimeSlowManager.registerSource(serverPlayer, TimeSlowManager.OS_SOURCE);
				}
			}

			@Override
			public void onUnequip(SlotContext slotContext, ItemStack newStack) {
				AccessoriesAPI.getCuriosInventory(slotContext.entity())
					.ifPresent(inventory -> {
						inventory.removeSlotModifier(
							OS_SKILL_SLOT, OS_SKILL_SLOTS_MODIFIER);
					});

				if (slotContext.entity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					TimeSlowManager.unregisterSource(serverPlayer, TimeSlowManager.OS_SOURCE);
				}
			}

			@Override
			public List<net.minecraft.network.chat.Component> getSlotsTooltip(
				List<net.minecraft.network.chat.Component> tooltips,
				net.minecraft.world.item.Item.TooltipContext context) {
				tooltips.add(net.minecraft.network.chat.Component.translatable(
					"equipment-plus.os.tooltip", getSkillSlotCount()));
				return tooltips;
			}
		};
	}
}

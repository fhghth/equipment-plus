package yuboobo.equipment.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;
import yuboobo.accessories.api.type.capability.ICurioItem;

import yuboobo.equipment.EquipmentPlus;

/**
 * Placeholder skill chip. Any item placed in an {@code os_skill} slot is treated as
 * a skill; chips may later be split into distinct behaviors.
 */
public class SkillChipItem extends Item implements ICurioItem {

	public SkillChipItem(Properties properties) {
		super(properties);
	}

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(SkillChipItem.this);
			}

			@Override
			public boolean canEquipFromUse(SlotContext slotContext) {
				return true;
			}
		};
	}
}

package yuboobo.equipment.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.type.capability.ICurio;
import yuboobo.accessories.api.type.capability.ICurioItem;

/**
 * Base class for OS skill chips. Concrete subclasses define distinct skills which are
 * dispatched in {@link yuboobo.equipment.skill.OSSkillManager}.
 */
public abstract class SkillChipItem extends Item implements ICurioItem {

	protected SkillChipItem(Properties properties) {
		super(properties);
	}

	/**
	 * The translation key of the tooltip line describing this chip's skill.
	 */
	protected abstract String getSkillTooltipKey();

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(SkillChipItem.this);
			}

			@Override
			public List<Component> getSlotsTooltip(List<Component> tooltips,
												   Item.TooltipContext context) {
				tooltips.add(Component.translatable(getSkillTooltipKey()));
				return tooltips;
			}
		};
	}
}
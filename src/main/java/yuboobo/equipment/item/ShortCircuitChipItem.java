package yuboobo.equipment.item;

import net.minecraft.world.item.Item;

/**
 * Short Circuit: deals direct damage to the aimed-at entity.
 */
public class ShortCircuitChipItem extends SkillChipItem {

	public ShortCircuitChipItem(Properties properties) {
		super(properties);
	}

	@Override
	protected String getSkillTooltipKey() {
		return "equipment-plus.chip.short_circuit.tooltip";
	}
}
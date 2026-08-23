package yuboobo.equipment.item;

import net.minecraft.world.item.Item;

/**
 * Contagion: poisons the aimed-at entity; the poison spreads to nearby enemies while
 * the effect lasts.
 */
public class ContagionChipItem extends SkillChipItem {

	public ContagionChipItem(Properties properties) {
		super(properties);
	}

	@Override
	protected String getSkillTooltipKey() {
		return "equipment-plus.chip.contagion.tooltip";
	}
}
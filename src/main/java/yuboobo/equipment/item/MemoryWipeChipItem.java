package yuboobo.equipment.item;

import net.minecraft.world.item.Item;

/**
 * Memory Wipe: the aimed-at mob loses its aggro on the player for a short time.
 */
public class MemoryWipeChipItem extends SkillChipItem {

	public MemoryWipeChipItem(Properties properties) {
		super(properties);
	}

	@Override
	protected String getSkillTooltipKey() {
		return "equipment-plus.chip.memory_wipe.tooltip";
	}
}
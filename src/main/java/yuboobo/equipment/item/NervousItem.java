package yuboobo.equipment.item;

import net.minecraft.world.item.Item;

import yuboobo.accessories.api.type.capability.ICurioItem;

/**
 * Marker base class for nervous-type accessories (they share the
 * {@code nervous_system} slot). Skill dispatch and client gating check
 * {@code instanceof NervousItem} so every variant works uniformly.
 */
public abstract class NervousItem extends Item implements ICurioItem {

	public static final String NERVOUS_SLOT = "nervous_system";

	protected NervousItem(Properties properties) {
		super(properties);
	}
}
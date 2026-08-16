package yuboobo.equipment.item;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;
import yuboobo.accessories.api.type.capability.ICurioItem;

import yuboobo.equipment.skill.NervousSlowManager;

/**
 * Wearable nervous system. Press the nervous system key (C by default) to slow
 * the world around the wearer for a short time - the wearer themselves keeps
 * full speed (Sandevistan style) - then the ability goes on cooldown.
 *
 * <p>Future nervous system variants can implement different abilities; the key
 * binding is per slot type, so the binding itself never changes.
 */
public class NervousSystemItem extends Item implements ICurioItem {

	public static final String NERVOUS_SLOT = "nervous_system";

	public NervousSystemItem(Properties properties) {
		super(properties);
	}

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(NervousSystemItem.this);
			}

			@Override
			public boolean canEquipFromUse(SlotContext slotContext) {
				return true;
			}

			@Override
			public void curioTick(SlotContext slotContext) {

				if (slotContext.entity() instanceof ServerPlayer serverPlayer) {
					NervousSlowManager.tick(serverPlayer);
				}
			}

			@Override
			public void onUnequip(SlotContext slotContext, ItemStack newStack) {

				if (slotContext.entity() instanceof ServerPlayer serverPlayer) {
					NervousSlowManager.remove(serverPlayer.getUUID());
				}
			}

			@Override
			public List<net.minecraft.network.chat.Component> getSlotsTooltip(
				List<net.minecraft.network.chat.Component> tooltips,
				net.minecraft.world.item.Item.TooltipContext context) {
				tooltips.add(net.minecraft.network.chat.Component.translatable(
					"equipment-plus.nervous.tooltip"));
				return tooltips;
			}
		};
	}
}

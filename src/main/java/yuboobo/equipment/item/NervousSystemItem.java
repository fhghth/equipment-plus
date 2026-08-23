package yuboobo.equipment.item;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;

import yuboobo.equipment.network.SNervousStatusPayload;
import yuboobo.equipment.skill.NervousSlowManager;

/**
 * Wearable nervous system (Sandevistan). Press the nervous system key (C by default)
 * to slow the world around the wearer for a short time - the wearer themselves keeps
 * full speed - then the ability goes on cooldown.
 *
 * <p>Each nervous variant declares its ability through
 * {@link yuboobo.equipment.skill.NervousAbilityRegistry}; adding a new variant only
 * requires a new item class plus a registry entry.
 */
public class NervousSystemItem extends NervousItem {

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
					NervousSlowManager.remove(serverPlayer);
					ServerPlayNetworking.send(serverPlayer,
						new SNervousStatusPayload(
							SNervousStatusPayload.VARIANT_SANDEVISTAN, false, 0));
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
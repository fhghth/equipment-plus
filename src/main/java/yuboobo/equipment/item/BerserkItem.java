package yuboobo.equipment.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.CurioAttributeModifiers;
import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.network.SNervousStatusPayload;
import yuboobo.equipment.skill.BerserkManager;

/**
 * Berserk nervous-system variant: grants {@code +10} attack damage while worn; the
 * nervous system key (C) triggers a short regeneration burst and a {@code +50%}
 * attack damage boost.
 */
public class BerserkItem extends NervousItem {

	public static final Identifier ATTACK_DAMAGE_MODIFIER_ID = EquipmentPlus.id("berserk_attack");
	private static final double ATTACK_DAMAGE_BONUS = 10.0D;

	public BerserkItem(Properties properties) {
		super(properties);
	}

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(BerserkItem.this);
			}

			@Override
			public CurioAttributeModifiers getDefaultCurioAttributeModifiers() {
				return CurioAttributeModifiers.builder()
					.addModifier(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, ATTACK_DAMAGE_BONUS,
							AttributeModifier.Operation.ADD_VALUE),
						NervousItem.NERVOUS_SLOT)
					.build();
			}

			@Override
			public boolean canEquipFromUse(SlotContext slotContext) {
				return true;
			}

			@Override
			public void onUnequip(SlotContext slotContext, ItemStack newStack) {
				if (slotContext.entity() instanceof Player player) {
					AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);

					if (attribute != null) {
						attribute.removeModifier(BerserkManager.ACTIVE_MODIFIER_ID);
					}

					if (player instanceof ServerPlayer serverPlayer) {
						BerserkManager.remove(serverPlayer.getUUID());
						ServerPlayNetworking.send(serverPlayer,
							new SNervousStatusPayload(
								SNervousStatusPayload.VARIANT_BERSERK, false, 0));
					}
				}
			}

			@Override
			public void curioTick(SlotContext slotContext) {
				if (slotContext.entity() instanceof ServerPlayer serverPlayer) {
					BerserkManager.tick(serverPlayer);
				}
			}

			@Override
			public List<Component> getSlotsTooltip(List<Component> tooltips,
												   net.minecraft.world.item.Item.TooltipContext context) {
				tooltips.add(Component.translatable("equipment-plus.berserk.tooltip"));
				return tooltips;
			}
		};
	}
}
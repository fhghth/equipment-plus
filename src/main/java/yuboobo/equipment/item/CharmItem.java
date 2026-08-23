package yuboobo.equipment.item;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yuboobo.accessories.api.CurioAttributeModifiers;
import yuboobo.accessories.api.SlotContext;
import yuboobo.accessories.api.type.capability.ICurio;
import yuboobo.accessories.api.type.capability.ICurioItem;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.network.SSkillStatusPayload;
import yuboobo.equipment.skill.SpeedBoostManager;

public class CharmItem extends Item implements ICurioItem {

	public static final Identifier HEALTH_MODIFIER_ID = EquipmentPlus.id("charm_health");
	private static final double MAX_HEALTH_BONUS = 10.0D;

	public CharmItem(Properties properties) {
		super(properties);
	}

	@Override
	public ICurio getCurio(ItemStack stack) {
		return new ICurio() {

			@Override
			public ItemStack getStack() {
				return new ItemStack(CharmItem.this);
			}

			@Override
			public CurioAttributeModifiers getDefaultCurioAttributeModifiers() {
				return CurioAttributeModifiers.builder()
					.addModifier(Attributes.MAX_HEALTH,
						new AttributeModifier(HEALTH_MODIFIER_ID, MAX_HEALTH_BONUS,
							AttributeModifier.Operation.ADD_VALUE),
						"charm")
					.build();
			}

			@Override
			public void onEquip(SlotContext slotContext, ItemStack prevStack) {
				if (slotContext.entity() instanceof Player player) {
					player.heal((float) MAX_HEALTH_BONUS);
				}
			}

			@Override
			public void onUnequip(SlotContext slotContext, ItemStack newStack) {

				if (slotContext.entity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					SpeedBoostManager.remove(serverPlayer.getUUID());
					ServerPlayNetworking.send(serverPlayer, new SSkillStatusPayload(false, 0));
				}
			}

			@Override
			public void curioTick(SlotContext slotContext) {
				LivingEntity entity = slotContext.entity();

				if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					SpeedBoostManager.tick(serverPlayer);
				}
			}

			@Override
			public List<Component> getSlotsTooltip(List<Component> tooltips,
												   net.minecraft.world.item.Item.TooltipContext context) {
				tooltips.add(Component.translatable("equipment-plus.charm.tooltip"));
				return tooltips;
			}
		};
	}
}

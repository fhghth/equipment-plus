package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

public record CSkillActivatePayload() implements CustomPacketPayload {

	public static final Type<CSkillActivatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "skill_activate"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CSkillActivatePayload> STREAM_CODEC =
		StreamCodec.unit(new CSkillActivatePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

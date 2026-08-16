package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

public record OSSkillActivatePayload(int skillIndex) implements CustomPacketPayload {

	public static final Type<OSSkillActivatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "os_skill_activate"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OSSkillActivatePayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OSSkillActivatePayload::skillIndex,
			OSSkillActivatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

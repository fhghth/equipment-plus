package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

public record SSkillStatusPayload(boolean activated, int cooldownSeconds)
	implements CustomPacketPayload {

	public static final Type<SSkillStatusPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "skill_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SSkillStatusPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.BOOL, SSkillStatusPayload::activated,
			ByteBufCodecs.VAR_INT, SSkillStatusPayload::cooldownSeconds,
			SSkillStatusPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

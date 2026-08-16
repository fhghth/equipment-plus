package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

/**
 * Server-to-client result of a nervous-system activation attempt.
 */
public record SNervousStatusPayload(boolean activated, int cooldownSeconds)
	implements CustomPacketPayload {

	public static final Type<SNervousStatusPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "nervous_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SNervousStatusPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.BOOL, SNervousStatusPayload::activated,
			ByteBufCodecs.VAR_INT, SNervousStatusPayload::cooldownSeconds,
			SNervousStatusPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

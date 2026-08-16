package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

/**
 * Client-to-server request to activate the nervous-system slow-motion ability.
 */
public record NervousActivatePayload() implements CustomPacketPayload {

	public static final Type<NervousActivatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "nervous_activate"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NervousActivatePayload> STREAM_CODEC =
		StreamCodec.unit(new NervousActivatePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

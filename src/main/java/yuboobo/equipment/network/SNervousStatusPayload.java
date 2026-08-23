package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

/**
 * Server-to-client result of a nervous-type activation attempt. The variant
 * distinguishes the equipped accessory so the client can show the right feedback
 * (and only apply the world-slow visual for the Sandevistan).
 */
public record SNervousStatusPayload(int variant, boolean activated, int cooldownSeconds)
	implements CustomPacketPayload {

	/** The Sandevistan world-slow ability. */
	public static final int VARIANT_SANDEVISTAN = 0;
	/** The Berserk regeneration + attack boost ability. */
	public static final int VARIANT_BERSERK = 1;

	public static final Type<SNervousStatusPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "nervous_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SNervousStatusPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SNervousStatusPayload::variant,
			ByteBufCodecs.BOOL, SNervousStatusPayload::activated,
			ByteBufCodecs.VAR_INT, SNervousStatusPayload::cooldownSeconds,
			SNervousStatusPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

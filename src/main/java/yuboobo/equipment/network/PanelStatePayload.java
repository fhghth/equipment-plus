package yuboobo.equipment.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import yuboobo.equipment.EquipmentPlus;

/**
 * Client-to-server notification that the OS skill panel was opened or closed,
 * used to drive the slow-motion effect.
 */
public record PanelStatePayload(boolean open) implements CustomPacketPayload {

	public static final Type<PanelStatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(EquipmentPlus.MOD_ID, "panel_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PanelStatePayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.BOOL, PanelStatePayload::open,
			PanelStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

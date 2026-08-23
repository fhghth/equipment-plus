package yuboobo.equipment;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yuboobo.accessories.api.AccessoriesAPI;

import yuboobo.equipment.item.CharmItem;
import yuboobo.equipment.item.BerserkItem;
import yuboobo.equipment.item.ContagionChipItem;
import yuboobo.equipment.item.MemoryWipeChipItem;
import yuboobo.equipment.item.NervousSystemItem;
import yuboobo.equipment.item.OperatingSystemItem;
import yuboobo.equipment.item.ShortCircuitChipItem;
import yuboobo.equipment.network.CSkillActivatePayload;
import yuboobo.equipment.network.NervousActivatePayload;
import yuboobo.equipment.network.OSSkillActivatePayload;
import yuboobo.equipment.network.PanelStatePayload;
import yuboobo.equipment.network.SNervousStatusPayload;
import yuboobo.equipment.network.SSkillStatusPayload;
import yuboobo.equipment.skill.BerserkManager;
import yuboobo.equipment.skill.ContagionManager;
import yuboobo.equipment.skill.MemoryWipeManager;
import yuboobo.equipment.skill.NervousAbility;
import yuboobo.equipment.skill.NervousAbilityRegistry;
import yuboobo.equipment.skill.NervousSlowManager;
import yuboobo.equipment.skill.OSSkillManager;
import yuboobo.equipment.skill.SpeedBoostManager;
import yuboobo.equipment.time.TimeSlowConfig;
import yuboobo.equipment.time.TimeSlowManager;

public class EquipmentPlus implements ModInitializer {
	public static final String MOD_ID = "equipment-plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final CharmItem CHARM = new CharmItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("charm")))
			.stacksTo(1));
	public static final OperatingSystemItem OPERATING_SYSTEM = new OperatingSystemItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("operating_system")))
			.stacksTo(1),
		2);
	public static final OperatingSystemItem ADVANCED_OPERATING_SYSTEM = new OperatingSystemItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("advanced_operating_system")))
			.stacksTo(1),
		3);
	public static final ShortCircuitChipItem SHORT_CIRCUIT_CHIP = new ShortCircuitChipItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("short_circuit_chip")))
			.stacksTo(64));
	public static final ContagionChipItem CONTAGION_CHIP = new ContagionChipItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("contagion_chip")))
			.stacksTo(64));
	public static final MemoryWipeChipItem MEMORY_WIPE_CHIP = new MemoryWipeChipItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("memory_wipe_chip")))
			.stacksTo(64));
	public static final NervousSystemItem NERVOUS_SYSTEM = new NervousSystemItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("nervous_system")))
			.stacksTo(1));
	public static final BerserkItem BERSERK = new BerserkItem(
		new Item.Properties()
			.setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.ITEM, id("berserk")))
			.stacksTo(1));

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, id("charm"), CHARM);
		Registry.register(BuiltInRegistries.ITEM, id("operating_system"), OPERATING_SYSTEM);
		Registry.register(BuiltInRegistries.ITEM, id("advanced_operating_system"),
			ADVANCED_OPERATING_SYSTEM);
		Registry.register(BuiltInRegistries.ITEM, id("nervous_system"), NERVOUS_SYSTEM);
		Registry.register(BuiltInRegistries.ITEM, id("berserk"), BERSERK);
		Registry.register(BuiltInRegistries.ITEM, id("short_circuit_chip"), SHORT_CIRCUIT_CHIP);
		Registry.register(BuiltInRegistries.ITEM, id("contagion_chip"), CONTAGION_CHIP);
		Registry.register(BuiltInRegistries.ITEM, id("memory_wipe_chip"), MEMORY_WIPE_CHIP);
		AccessoriesAPI.registerCurio(CHARM, CHARM);
		AccessoriesAPI.registerCurio(OPERATING_SYSTEM, OPERATING_SYSTEM);
		AccessoriesAPI.registerCurio(ADVANCED_OPERATING_SYSTEM, ADVANCED_OPERATING_SYSTEM);
		AccessoriesAPI.registerCurio(NERVOUS_SYSTEM, NERVOUS_SYSTEM);
		AccessoriesAPI.registerCurio(BERSERK, BERSERK);

		NervousAbilityRegistry.register(NervousSystemItem.class,
			NervousAbility.of(SNervousStatusPayload.VARIANT_SANDEVISTAN,
				NervousSlowManager::tryActivate,
				NervousSlowManager::getCooldownRemainingTicks));
		NervousAbilityRegistry.register(BerserkItem.class,
			NervousAbility.of(SNervousStatusPayload.VARIANT_BERSERK,
				BerserkManager::tryActivate,
				BerserkManager::getCooldownRemainingTicks));
		AccessoriesAPI.registerCurio(SHORT_CIRCUIT_CHIP, SHORT_CIRCUIT_CHIP);
		AccessoriesAPI.registerCurio(CONTAGION_CHIP, CONTAGION_CHIP);
		AccessoriesAPI.registerCurio(MEMORY_WIPE_CHIP, MEMORY_WIPE_CHIP);

		PayloadTypeRegistry.serverboundPlay().register(CSkillActivatePayload.TYPE,
			CSkillActivatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OSSkillActivatePayload.TYPE,
			OSSkillActivatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PanelStatePayload.TYPE,
			PanelStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NervousActivatePayload.TYPE,
			NervousActivatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SSkillStatusPayload.TYPE,
			SSkillStatusPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SNervousStatusPayload.TYPE,
			SNervousStatusPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OSSkillActivatePayload.TYPE,
			(payload, context) -> context.server().execute(() ->
				OSSkillManager.execute(context.player(), payload.skillIndex())));

		ServerPlayNetworking.registerGlobalReceiver(PanelStatePayload.TYPE,
			(payload, context) -> context.server().execute(() ->
				TimeSlowManager.setPanelOpen(context.player(), payload.open())));

		ServerPlayNetworking.registerGlobalReceiver(NervousActivatePayload.TYPE,
			(payload, context) -> context.server().execute(() -> {
				var player = context.player();
				NervousAbilityRegistry.Result result = NervousAbilityRegistry.activate(player);

				if (result == null) {
					return;
				}

				ServerPlayNetworking.send(player,
					new SNervousStatusPayload(result.ability().variant(),
						result.activated(), result.cooldownTicks() / 20));
			}));

		ServerPlayNetworking.registerGlobalReceiver(CSkillActivatePayload.TYPE,
			(payload, context) -> context.server().execute(() -> {
				var player = context.player();
				boolean activated = SpeedBoostManager.tryActivate(player);
				int cooldown = SpeedBoostManager.getCooldownRemainingTicks(player);

				ServerPlayNetworking.send(player,
					new SSkillStatusPayload(activated, cooldown / 20));
			}));

		TimeSlowConfig.init();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
				ContagionManager.tick(level);
				MemoryWipeManager.tick(level);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> TimeSlowManager.tick());
		ServerLifecycleEvents.SERVER_STARTING.register(server -> TimeSlowManager.onServerStart(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TimeSlowManager.onServerStop());
		ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> {
				SpeedBoostManager.remove(handler.player.getUUID());
				NervousSlowManager.remove(handler.player);
				TimeSlowManager.removePlayer(handler.player);
			});
		ServerEntityEvents.ENTITY_UNLOAD.register(
			(entity, world) -> {
				if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
					SpeedBoostManager.remove(player.getUUID());
					NervousSlowManager.remove(player);
					TimeSlowManager.removePlayer(player);
				}
			});

		LOGGER.info("Equipment Plus initialized (accessories-api integration)");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

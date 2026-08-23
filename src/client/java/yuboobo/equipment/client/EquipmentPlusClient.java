package yuboobo.equipment.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import yuboobo.accessories.api.AccessoriesAPI;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.client.hud.EffectHud;
import yuboobo.equipment.client.hud.EffectHudEntry;
import yuboobo.equipment.item.NervousItem;
import yuboobo.equipment.network.CSkillActivatePayload;
import yuboobo.equipment.network.NervousActivatePayload;
import yuboobo.equipment.network.SNervousStatusPayload;
import yuboobo.equipment.network.SSkillStatusPayload;
import yuboobo.equipment.skill.BerserkManager;
import yuboobo.equipment.skill.NervousSlowManager;
import yuboobo.equipment.skill.SpeedBoostManager;

public class EquipmentPlusClient implements ClientModInitializer {

	public static final KeyMapping.Category SKILL_CATEGORY =
		new KeyMapping.Category(EquipmentPlus.id("key.equipment-plus.category"));

	/** Charm and other independent-skill accessories. */
	private static final KeyMapping SKILL_KEY = new KeyMapping(
		"key.equipment-plus.skill",
		GLFW.GLFW_KEY_K,
		SKILL_CATEGORY);

	/** Hold to open the operating-system skill panel. */
	public static final KeyMapping OS_PANEL_KEY = new KeyMapping(
		"key.equipment-plus.os_panel",
		GLFW.GLFW_KEY_TAB,
		SKILL_CATEGORY);

	/** Press to activate the equipped nervous system's ability (world slow). */
	private static final KeyMapping NERVOUS_KEY = new KeyMapping(
		"key.equipment-plus.nervous",
		GLFW.GLFW_KEY_C,
		SKILL_CATEGORY);

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(SKILL_KEY);
		KeyMappingHelper.registerKeyMapping(OS_PANEL_KEY);
		KeyMappingHelper.registerKeyMapping(NERVOUS_KEY);

		EffectHud.register("speed", new EffectHudEntry(
			Component.translatable("equipment-plus.hud.speed"),
			EquipmentPlus.CHARM,
			SpeedBoostManager.BOOST_DURATION_TICKS, SpeedBoostManager.COOLDOWN_TICKS));
		EffectHud.register("sandevistan", new EffectHudEntry(
			Component.translatable("equipment-plus.hud.sandevistan"),
			EquipmentPlus.NERVOUS_SYSTEM,
			NervousSlowManager.SLOW_DURATION_TICKS, NervousSlowManager.COOLDOWN_TICKS));
		EffectHud.register("berserk", new EffectHudEntry(
			Component.translatable("equipment-plus.hud.berserk"),
			EquipmentPlus.BERSERK,
			BerserkManager.DURATION_TICKS, BerserkManager.COOLDOWN_TICKS));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SKILL_KEY.consumeClick()) {
				ClientPlayNetworking.send(new CSkillActivatePayload());
			}
			while (NERVOUS_KEY.consumeClick()) {
				if (hasNervousSystem(client)) {
					ClientPlayNetworking.send(new NervousActivatePayload());
				}
			}
			OSPanelManager.tick(client);
			TimeSlowClient.tick();
			EffectHud.tick();
		});

		ClientPlayNetworking.registerGlobalReceiver(SSkillStatusPayload.TYPE,
			(payload, context) -> context.client().execute(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				EffectHudEntry entry = EffectHud.get("speed");

				if (entry != null) {

					if (payload.activated()) {
						entry.activate();
					} else if (payload.cooldownSeconds() == 0) {
						entry.reset();
					} else {
						entry.setCooldown(payload.cooldownSeconds() * 20);
					}
				}

				if (minecraft.player != null
					&& (payload.activated() || payload.cooldownSeconds() > 0)) {
					Component message = payload.activated()
						? Component.translatable("equipment-plus.skill.activated")
						: Component.translatable("equipment-plus.skill.cooldown",
							payload.cooldownSeconds());
					minecraft.player.sendSystemMessage(message);
				}
			}));

		ClientPlayNetworking.registerGlobalReceiver(SNervousStatusPayload.TYPE,
			(payload, context) -> context.client().execute(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				boolean berserk = payload.variant() == SNervousStatusPayload.VARIANT_BERSERK;
				EffectHudEntry entry = EffectHud.get(berserk ? "berserk" : "sandevistan");

				if (entry != null) {

					if (payload.activated()) {
						entry.activate();
					} else if (payload.cooldownSeconds() == 0) {
						entry.reset();
					} else {
						entry.setCooldown(payload.cooldownSeconds() * 20);
					}
				}

				if (!berserk && payload.activated()) {
					TimeSlowClient.activateNervousSlow();
				}

				if (minecraft.player != null
					&& (payload.activated() || payload.cooldownSeconds() > 0)) {
					Component message = payload.activated()
						? Component.translatable(berserk
							? "equipment-plus.berserk.activated"
							: "equipment-plus.nervous.activated")
						: Component.translatable(berserk
							? "equipment-plus.berserk.cooldown"
							: "equipment-plus.nervous.cooldown",
							payload.cooldownSeconds());
					minecraft.player.sendSystemMessage(message);
				}
			}));

		HudElementRegistry.addLast(EquipmentPlus.id("os_panel"),
			OSPanelManager::render);
		HudElementRegistry.addLast(EquipmentPlus.id("effect_hud"),
			EffectHud::render);
		LevelRenderEvents.BEFORE_GIZMOS.register(
			context -> OSPanelManager.renderTargetHighlight());
	}

	private static boolean hasNervousSystem(Minecraft minecraft) {
		return minecraft.player != null
			&& AccessoriesAPI.getCuriosInventoryOrNull(minecraft.player) != null
			&& AccessoriesAPI.getCuriosInventoryOrNull(minecraft.player)
				.findFirstCurio(
					stack -> stack.getItem() instanceof NervousItem).isPresent();
	}
}

package yuboobo.equipment.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;
import yuboobo.accessories.api.type.inventory.IDynamicStackHandler;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.item.OperatingSystemItem;
import yuboobo.equipment.network.OSSkillActivatePayload;
import yuboobo.equipment.network.PanelStatePayload;

/**
 * Client-side state machine for the operating-system skill panel.
 *
 * <p>While the player holds the OS panel key (Tab) and wears an operating system, a
 * semi-transparent panel is drawn on the left side of the screen listing the equipped
 * OS skills. The scroll wheel cycles the selection, a left click releases (fires) the
 * selected skill, and releasing Tab closes the panel. While the panel is open the living
 * entity under the crosshair is highlighted in the world.
 */
public final class OSPanelManager {

	/** Must match the server side {@code OSSkillManager} raycast range. */
	private static final double RAYCAST_RANGE = 32.0D;

	private static final int PANEL_WIDTH = 124;
	private static final int PANEL_PADDING = 6;
	private static final int TITLE_HEIGHT = 11;
	private static final int ENTRY_HEIGHT = 20;
	private static final int ENTRY_SPACING = 2;
	private static final int FOOTER_HEIGHT = 10;
	private static final int EMPTY_HINT_HEIGHT = 14;

	/** ARGB colors used by the panel. */
	private static final int COLOR_BACKGROUND = 0x99000000;
	private static final int COLOR_BORDER = 0xCCFFFFFF;
	private static final int COLOR_TITLE = 0xFF9FE8FF;
	private static final int COLOR_SELECTED_BG = 0x66FFFFFF;
	private static final int COLOR_ENTRY_BG = 0x40000000;
	private static final int COLOR_NAME_SELECTED = 0xFFFFFFFF;
	private static final int COLOR_NAME = 0xFFDDDDDD;
	private static final int COLOR_FOOTER = 0xFF999999;
	private static final int COLOR_HINT = 0xFF666666;
	private static final int COLOR_HIGHLIGHT = 0xFFFF4545;

	private static boolean open;
	private static int selectedSlot = -1;
	private static double scrollAccumulator;
	private static Entity target;
	private static final List<SkillEntry> skills = new ArrayList<>();

	private record SkillEntry(int slot, ItemStack stack) {
	}

	private OSPanelManager() {
	}

	public static boolean isOpen() {
		return open;
	}

	/**
	 * Called every client tick from {@link EquipmentPlusClient}.
	 */
	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.level == null) {
			close();
			return;
		}

		boolean shouldOpen = minecraft.gui.screen() == null
			&& EquipmentPlusClient.OS_PANEL_KEY.isDown() && hasOperatingSystem(player);

		if (shouldOpen != open) {
			if (shouldOpen) {
				open();
			} else {
				close();
			}
		}

		if (!open) {
			return;
		}

		refreshSkills(player);
		consumeScroll();
		updateTarget(minecraft.level, player);
	}

	/**
	 * Called from the {@code MouseHandler} mixin while the panel is open.
	 */
	public static void onScroll(double verticalAmount) {
		if (open) {
			scrollAccumulator += verticalAmount;
		}
	}

	/**
	 * Called from the {@code Minecraft} mixin when the player left clicks while the panel
	 * is open; fires the selected skill.
	 */
	public static void onAttackClick() {
		if (!open || selectedSlot < 0) {
			return;
		}
		ClientPlayNetworking.send(new OSSkillActivatePayload(selectedSlot));
	}

	/**
	 * Renders the skill panel as a HUD element.
	 */
	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!open) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int guiWidth = graphics.guiWidth();
		int guiHeight = graphics.guiHeight();

		int contentHeight = skills.isEmpty()
			? EMPTY_HINT_HEIGHT
			: skills.size() * (ENTRY_HEIGHT + ENTRY_SPACING) - ENTRY_SPACING;
		int panelHeight = PANEL_PADDING * 2 + TITLE_HEIGHT + contentHeight + FOOTER_HEIGHT;
		int panelX = 10;
		int panelY = guiHeight / 2 - panelHeight / 2;
		int panelRight = panelX + PANEL_WIDTH;
		int panelBottom = panelY + panelHeight;

		// Semi-transparent background
		graphics.fill(panelX, panelY, panelRight, panelBottom, COLOR_BACKGROUND);
		// Border
		graphics.horizontalLine(panelX, panelRight - 1, panelY, COLOR_BORDER);
		graphics.horizontalLine(panelX, panelRight - 1, panelBottom - 1, COLOR_BORDER);
		graphics.verticalLine(panelX, panelY, panelBottom - 1, COLOR_BORDER);
		graphics.verticalLine(panelRight - 1, panelY, panelBottom - 1, COLOR_BORDER);

		// Title
		graphics.text(font,
			Component.translatable("equipment-plus.os.panel.title"),
			panelX + 8, panelY + 4, COLOR_TITLE);

		// Skill entries
		int y = panelY + PANEL_PADDING + TITLE_HEIGHT;

		if (skills.isEmpty()) {
			graphics.text(font,
				Component.translatable("equipment-plus.os.panel.no_skills"),
				panelX + 8, y + 2, COLOR_HINT);
		} else {
			for (SkillEntry entry : skills) {
				boolean selected = entry.slot() == selectedSlot;

				graphics.fill(panelX + 3, y, panelRight - 3, y + ENTRY_HEIGHT,
					selected ? COLOR_SELECTED_BG : COLOR_ENTRY_BG);

				if (selected) {
					graphics.horizontalLine(panelX + 3, panelRight - 4, y, COLOR_NAME_SELECTED);
					graphics.horizontalLine(panelX + 3, panelRight - 4, y + ENTRY_HEIGHT - 1,
						COLOR_NAME_SELECTED);
					graphics.verticalLine(panelX + 3, y, y + ENTRY_HEIGHT - 1,
						COLOR_NAME_SELECTED);
					graphics.verticalLine(panelRight - 4, y, y + ENTRY_HEIGHT - 1,
						COLOR_NAME_SELECTED);
				}

				graphics.item(entry.stack(), panelX + 7, y + 2);
				graphics.text(font, entry.stack().getHoverName(), panelX + 27, y + 6,
					selected ? COLOR_NAME_SELECTED : COLOR_NAME);
				y += ENTRY_HEIGHT + ENTRY_SPACING;
			}
		}

		// Footer hint
		graphics.text(font,
			Component.translatable("equipment-plus.os.panel.hint"),
			panelX + 8, panelBottom - FOOTER_HEIGHT, COLOR_FOOTER);
	}

	/**
	 * Called from the {@code LevelRenderEvents.BEFORE_GIZMOS} event while the panel is
	 * open; draws a box and a name tag around the targeted entity.
	 */
	public static void renderTargetHighlight() {
		if (!open || target == null) {
			return;
		}

		if (target.isRemoved() || !target.isAlive()) {
			target = null;
			return;
		}

		Gizmos.cuboid(target.getBoundingBox().inflate(0.12D),
			GizmoStyle.stroke(COLOR_HIGHLIGHT, 2.5F)).setAlwaysOnTop();
		Gizmos.billboardTextOverMob(target, 0,
			target.getDisplayName().getString(), COLOR_HIGHLIGHT, 0.55F);
	}

	private static void open() {
		if (open) {
			return;
		}
		open = true;
		scrollAccumulator = 0.0D;
		selectedSlot = -1;
		target = null;
		skills.clear();
		sendPanelState(true);
	}

	private static void close() {
		if (!open) {
			return;
		}
		open = false;
		selectedSlot = -1;
		scrollAccumulator = 0.0D;
		target = null;
		skills.clear();
		sendPanelState(false);
	}

	private static void sendPanelState(boolean state) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player != null && minecraft.level != null) {
			ClientPlayNetworking.send(new PanelStatePayload(state));
		}
	}

	private static boolean hasOperatingSystem(LocalPlayer player) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);
		return inventory != null
			&& inventory.findFirstCurio(EquipmentPlus.OPERATING_SYSTEM).isPresent();
	}

	private static void refreshSkills(LocalPlayer player) {
		skills.clear();
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);

		if (inventory == null) {
			return;
		}
		inventory.getStacksHandler(OperatingSystemItem.OS_SKILL_SLOT).ifPresent(handler -> {
			IDynamicStackHandler stacks = handler.getStacks();

			for (int i = 0; i < stacks.getSlots(); i++) {
				ItemStack stack = stacks.getStackInSlot(i);

				if (!stack.isEmpty()) {
					skills.add(new SkillEntry(i, stack));
				}
			}
		});

		if (skills.isEmpty()) {
			selectedSlot = -1;
		} else if (indexOfSlot(selectedSlot) < 0) {
			selectedSlot = skills.getFirst().slot();
		}
	}

	private static void consumeScroll() {
		int steps = (int) scrollAccumulator;

		if (steps == 0 || skills.size() <= 1) {
			scrollAccumulator = 0.0D;
			return;
		}
		scrollAccumulator -= steps;

		int current = Math.max(0, indexOfSlot(selectedSlot));
		int next = Math.floorMod(current - steps, skills.size());
		selectedSlot = skills.get(next).slot();
	}

	private static int indexOfSlot(int slot) {
		for (int i = 0; i < skills.size(); i++) {
			if (skills.get(i).slot() == slot) {
				return i;
			}
		}
		return -1;
	}

	private static void updateTarget(ClientLevel level, LocalPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().scale(RAYCAST_RANGE);
		Vec3 end = eye.add(look);

		if (level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			target = null;
			return;
		}

		AABB search = player.getBoundingBox().expandTowards(look).inflate(1.0D);
		List<Entity> entities = level.getEntities(player, search,
			entity -> entity instanceof LivingEntity && entity.isAlive());
		double best = Double.MAX_VALUE;
		Entity hit = null;

		for (Entity entity : entities) {
			Optional<Vec3> point = entity.getBoundingBox().inflate(0.3D).clip(eye, end);

			if (point.isPresent()) {
				double distance = eye.distanceToSqr(point.get());

				if (distance < best) {
					best = distance;
					hit = entity;
				}
			}
		}
		target = hit;
	}
}

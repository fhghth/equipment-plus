package yuboobo.equipment.client.hud;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import yuboobo.equipment.EquipmentPlus;

/**
 * Generic HUD tracker for non-vanilla effect durations and cooldowns. Any gameplay
 * system can register an {@link EffectHudEntry} ({@link #register(EffectHudEntry)}),
 * drive it through {@link #tick()} every client tick and draw it with
 * {@link #render(GuiGraphicsExtractor, DeltaTracker)}.
 *
 * <p>While active, a single icon is shown at the top-left corner with a red border and
 * a translucent pale-cyan overlay that drains away with the remaining duration. Once the
 * duration ends the icon stays and a translucent dark fan sweeps in from the 12 o'clock
 * position, rotating clockwise over the icon; when the fan has fully covered the icon the
 * cooldown is over and the icon disappears. Vanilla potion effects are left untouched.
 *
 * <p>The fan is drawn with plain per-column fills computed in screen space; the
 * extractor's render phase does not apply non-identity pose matrices reliably, so no
 * rotation is used.
 */
public final class EffectHud {

	private static final int ICON_SIZE = 16;
	private static final int ICON_PITCH = 18;
	private static final int TEXT_X = 8;
	private static final int TEXT_Y = 8;
	private static final int ICON_RADIUS = ICON_SIZE / 2;

	private static final int RED_OUTLINE_COLOR = 0xFFFF3030;
	private static final int CYAN_MASK_COLOR = 0x99C5E8FF;
	private static final int SWEEP_COLOR = 0xCC2A2A2A;

	private static final Map<String, EffectHudEntry> ENTRIES = new LinkedHashMap<>();

	private static final Map<String, String> LAST_STATES = new HashMap<>();

	private EffectHud() {
	}

	/**
	 * Registers an effect entry to be tracked and rendered. Duplicate ids are ignored.
	 *
	 * @param id    The unique id of the effect
	 * @param entry The entry to register
	 * @return The registered entry
	 */
	public static EffectHudEntry register(String id, EffectHudEntry entry) {
		ENTRIES.putIfAbsent(id, entry);
		return ENTRIES.get(id);
	}

	/**
	 * Retrieves a previously registered entry.
	 *
	 * @param id The id of the effect
	 * @return The entry, or null if it was never registered
	 */
	public static EffectHudEntry get(String id) {
		return ENTRIES.get(id);
	}

	/** Ticks every registered entry; call once per client tick. */
	public static void tick() {
		ENTRIES.values().forEach(EffectHudEntry::tick);
	}

	/**
	 * Renders the effect icons at the top-left corner; use as a HUD element callback.
	 */
	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.gui.hud.isHidden()) {
			return;
		}

		int x = TEXT_X;
		int y = TEXT_Y;
		for (Map.Entry<String, EffectHudEntry> pair : ENTRIES.entrySet()) {
			String id = pair.getKey();
			EffectHudEntry entry = pair.getValue();
			boolean active = entry.isActive();
			boolean cooling = entry.isCoolingDown();
			String state = active ? "ACTIVE" : (cooling ? "COOLING" : "IDLE");

			if (!Objects.equals(state, LAST_STATES.put(id, state))) {
				EquipmentPlus.LOGGER.info("EffectHud[{}] -> {} (dur={}, cd={})",
					id, state, entry.getDurationTicks(), entry.getCooldownTicks());
			}

			if (active) {
				renderActive(graphics, entry, x, y);
			} else if (cooling && entry.getCooldownTicks() <= fanWindowTicks(entry)) {
				renderCooling(graphics, entry, x, y);
			} else {
				continue;
			}
			y += ICON_PITCH;
		}
	}

	private static void renderActive(GuiGraphicsExtractor graphics, EffectHudEntry entry,
									 int x, int y) {
		graphics.item(entry.getIcon(), x, y);
		graphics.outline(x - 1, y - 1, x + ICON_SIZE, y + ICON_SIZE, RED_OUTLINE_COLOR);

		float remaining = fraction(entry.getDurationTicks(), entry.getMaxDurationTicks());
		int maskHeight = Math.round(ICON_SIZE * remaining);
		if (maskHeight > 0) {
			graphics.fill(x, y + ICON_SIZE - maskHeight, x + ICON_SIZE, y + ICON_SIZE,
				CYAN_MASK_COLOR);
		}
	}

	private static void renderCooling(GuiGraphicsExtractor graphics, EffectHudEntry entry,
									  int x, int y) {
		graphics.item(entry.getIcon(), x, y);

		int windowTicks = fanWindowTicks(entry);
		float remaining = fraction(entry.getCooldownTicks(), windowTicks);
		float sweepDegrees = (1.0F - remaining) * 360.0F;
		float sweepRadians = (float) Math.toRadians(sweepDegrees);
		int centerX = Math.round(x + ICON_SIZE / 2.0F);
		int centerY = Math.round(y + ICON_SIZE / 2.0F);

		for (int dx = -ICON_RADIUS; dx <= ICON_RADIUS; dx++) {
			int columnX = centerX + dx;
			int runStart = -1;

			for (int dy = -ICON_RADIUS; dy <= ICON_RADIUS; dy++) {
				int columnY = centerY + dy;
				boolean covered = dx * dx + dy * dy <= ICON_RADIUS * ICON_RADIUS
					&& swept(centerX, centerY, columnX, columnY, sweepRadians);

				if (covered && runStart < 0) {
					runStart = columnY;
				} else if (!covered && runStart >= 0) {
					graphics.fill(columnX, runStart, columnX + 1, columnY, SWEEP_COLOR);
					runStart = -1;
				}
			}
			if (runStart >= 0) {
				graphics.fill(columnX, runStart, columnX + 1, centerY + ICON_RADIUS + 1,
					SWEEP_COLOR);
			}
		}
	}

	/**
	 * True when the pixel is inside the swept fan: its clockwise angle from the
	 * 12 o'clock position must be at most the swept angle.
	 */
	private static boolean swept(int centerX, int centerY, int pixelX, int pixelY,
								 float sweepRadians) {
		float a = (float) Math.atan2(pixelY - centerY, pixelX - centerX);
		float t = a + (float) Math.PI / 2.0F;
		if (t < 0.0F) {
			t += (float) Math.PI * 2.0F;
		}
		return t <= sweepRadians;
	}

	private static int fanWindowTicks(EffectHudEntry entry) {
		return entry.getMaxCooldownTicks() - entry.getMaxDurationTicks();
	}

	private static float fraction(int ticks, int maxTicks) {
		return maxTicks <= 0 ? 0.0F
			: Math.max(0.0F, Math.min(1.0F, ticks / (float) maxTicks));
	}
}
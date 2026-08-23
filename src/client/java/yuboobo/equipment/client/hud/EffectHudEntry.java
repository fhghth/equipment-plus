package yuboobo.equipment.client.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A single client-side tracked effect: how long it is active and how long until it can
 * be used again. Both counters count down in ticks; nothing here talks to the server,
 * it is purely driven by the payload receivers via {@link #activate()} and
 * {@link #setCooldown(int)}.
 *
 * <p>The icon is created lazily from the given item on first use: item stacks can only
 * be built after registries are bound, which never happens during an entrypoint.
 */
public final class EffectHudEntry {

	private final Component title;
	private final Item iconItem;
	private ItemStack icon;
	private final int maxDurationTicks;
	private final int maxCooldownTicks;
	private int durationTicks;
	private int cooldownTicks;

	public EffectHudEntry(Component title, Item iconItem, int maxDurationTicks,
						  int maxCooldownTicks) {
		this.title = title;
		this.iconItem = iconItem;
		this.maxDurationTicks = maxDurationTicks;
		this.maxCooldownTicks = maxCooldownTicks;
	}

	public Component getTitle() {
		return this.title;
	}

	public ItemStack getIcon() {
		if (this.icon == null) {
			this.icon = new ItemStack(this.iconItem);
		}
		return this.icon;
	}

	/** The ability was used: both the effect period and the cooldown start now. */
	public void activate() {
		this.durationTicks = this.maxDurationTicks;
		this.cooldownTicks = this.maxCooldownTicks;
	}

	/** The use was rejected: the server told us how many ticks remain. */
	public void setCooldown(int ticks) {
		this.cooldownTicks = Math.max(this.cooldownTicks, ticks);
	}

	/** The accessory was unequipped: clear both timers immediately. */
	public void reset() {
		this.durationTicks = 0;
		this.cooldownTicks = 0;
	}

	public boolean isActive() {
		return this.durationTicks > 0;
	}

	public boolean isCoolingDown() {
		return this.cooldownTicks > 0;
	}

	public int getDurationTicks() {
		return this.durationTicks;
	}

	public int getCooldownTicks() {
		return this.cooldownTicks;
	}

	public int getMaxDurationTicks() {
		return this.maxDurationTicks;
	}

	public int getMaxCooldownTicks() {
		return this.maxCooldownTicks;
	}

	public void tick() {
		this.durationTicks = Math.max(0, this.durationTicks - 1);
		this.cooldownTicks = Math.max(0, this.cooldownTicks - 1);
	}
}
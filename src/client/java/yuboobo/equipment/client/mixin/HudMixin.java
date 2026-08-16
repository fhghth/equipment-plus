package yuboobo.equipment.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

import yuboobo.equipment.client.OSPanelManager;

/**
 * Suppresses the vanilla Tab player list while the OS skill panel is open.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

	@Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$extractTabList(GuiGraphicsExtractor graphics,
											  DeltaTracker deltaTracker, CallbackInfo ci) {
		if (OSPanelManager.isOpen()) {
			((Hud) (Object) this).getTabList().setVisible(false);
			ci.cancel();
		}
	}
}

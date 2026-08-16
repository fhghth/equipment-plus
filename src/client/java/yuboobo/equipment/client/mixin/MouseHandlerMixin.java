package yuboobo.equipment.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MouseHandler;

import yuboobo.equipment.client.OSPanelManager;

/**
 * While the OS skill panel is open, the scroll wheel selects skills instead of cycling
 * the hotbar.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$onScroll(long windowPointer, double horizontalAmount,
										double verticalAmount, CallbackInfo ci) {
		if (OSPanelManager.isOpen()) {
			OSPanelManager.onScroll(verticalAmount);
			ci.cancel();
		}
	}
}

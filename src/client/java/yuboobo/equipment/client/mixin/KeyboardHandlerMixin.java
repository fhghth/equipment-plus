package yuboobo.equipment.client.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

import yuboobo.equipment.client.OSPanelManager;

/**
 * While the OS skill panel is open, the hotbar number keys 1..9 fire the skill at the
 * matching position in the panel list (top to bottom) instead of switching the hotbar.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$keyPress(long windowPointer, int action, KeyEvent event,
										CallbackInfo ci) {
		if (action != GLFW.GLFW_PRESS || !OSPanelManager.isOpen()) {
			return;
		}

		int key = event.key();

		if (key < GLFW.GLFW_KEY_1 || key > GLFW.GLFW_KEY_9) {
			return;
		}

		if (OSPanelManager.tryFireSlotIndex(key - GLFW.GLFW_KEY_1)) {
			ci.cancel();
		}
	}
}
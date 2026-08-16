package yuboobo.equipment.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;

import yuboobo.equipment.client.OSPanelManager;

/**
 * While the OS skill panel is open, left clicks fire the selected skill instead of the
 * vanilla attack, and holding the attack button no longer attacks.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$startAttack(CallbackInfoReturnable<Boolean> cir) {
		if (OSPanelManager.isOpen()) {
			OSPanelManager.onAttackClick();
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$continueAttack(boolean leftClick, CallbackInfo ci) {
		if (OSPanelManager.isOpen()) {
			ci.cancel();
		}
	}
}

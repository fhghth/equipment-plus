package yuboobo.equipment.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import yuboobo.equipment.client.TimeSlowClient;

/**
 * While the world is slowed, client-side arrows stop their local tick (which would
 * otherwise move them at the initial full speed) and are rendered purely from the
 * server position packets, giving a smooth slow-motion flight.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$clientSlowTick(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;

		if (self.level().isClientSide()
			&& TimeSlowClient.isSlowActive()
			&& TimeSlowClient.inRange(self)) {
			ci.cancel();
		}
	}
}

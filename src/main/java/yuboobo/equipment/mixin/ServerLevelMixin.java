package yuboobo.equipment.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import yuboobo.equipment.time.TimeSlowManager;

/**
 * Server-side slow-motion.
 *
 * <p>Living entities inside an active slow zone have their tick throttled to
 * {@code 1/factor} of the normal rate (phase-offset by entity id), so their
 * movement, AI and attacks slow down. While a tick is skipped, the damage timers
 * (invulnerability frames, hurt flash, death animation) still advance at real
 * time, so player attacks keep landing every swing.
 *
 * <p>Physics entities (projectiles, items, vehicles) are not throttled; their
 * tick runs every tick with the velocity temporarily scaled to the factor, giving
 * a smooth slow-motion flight without corrupting their velocity.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void equipmentPlus$throttleLivingTick(Entity entity, CallbackInfo ci) {

		if (TimeSlowManager.isThrottled(entity) && TimeSlowManager.shouldSkipTick(entity)) {

			if (entity instanceof LivingEntity living) {

				if (living.invulnerableTime > 0) {
					living.invulnerableTime--;
				}

				if (living.hurtTime > 0) {
					living.hurtTime--;
				}

				if (living.deathTime > 0) {
					living.deathTime++;
				}
			}
			ci.cancel();
		}
	}

	@WrapOperation(method = "tickNonPassenger",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
	private void equipmentPlus$slowPhysicsTick(Entity entity, Operation<Void> original) {

		if (entity instanceof LivingEntity) {
			original.call(entity);
			return;
		}
		double factor = TimeSlowManager.getSlowFactor(entity);

		if (factor >= 1.0D) {
			original.call(entity);
			return;
		}
		Vec3 originalDelta = entity.getDeltaMovement();
		entity.setDeltaMovement(originalDelta.scale(factor));

		try {
			original.call(entity);
		} finally {
			entity.setDeltaMovement(originalDelta);
		}
	}
}

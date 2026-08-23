package yuboobo.equipment.skill;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;
import yuboobo.accessories.api.type.inventory.ICurioStacksHandler;
import yuboobo.accessories.api.type.inventory.IDynamicStackHandler;

import yuboobo.equipment.item.ContagionChipItem;
import yuboobo.equipment.item.MemoryWipeChipItem;
import yuboobo.equipment.item.OperatingSystemItem;
import yuboobo.equipment.item.ShortCircuitChipItem;

public class OSSkillManager {

	private static final double RAYCAST_RANGE = 32.0D;
	private static final float SHORT_CIRCUIT_DAMAGE = 6.0F;

	private OSSkillManager() {
	}

	/**
	 * Executes the skill in the given {@code os_skill} slot index. Returns true when a
	 * skill was actually fired.
	 */
	public static boolean execute(ServerPlayer player, int index) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);

		if (inventory == null
			|| inventory.findFirstCurio(
				stack -> stack.getItem() instanceof OperatingSystemItem).isEmpty()) {
			player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.no_os"));
			return false;
		}

		Optional<ICurioStacksHandler> handler = inventory.getStacksHandler(OperatingSystemItem.OS_SKILL_SLOT);

		if (handler.isEmpty()) {
			return false;
		}

		IDynamicStackHandler stacks = handler.get().getStacks();

		if (index < 0 || index >= stacks.getSlots() || stacks.getStackInSlot(index).isEmpty()) {
			player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.empty"));
			return false;
		}

		ItemStack skill = stacks.getStackInSlot(index);
		ServerLevel level = player.level();

		if (skill.getItem() instanceof ShortCircuitChipItem) {
			LivingEntity target = findTarget(player);

			if (target != null) {
				target.hurtServer(level, player.damageSources().generic(), SHORT_CIRCUIT_DAMAGE);
				player.sendSystemMessage(
					Component.translatable("equipment-plus.os_skill.short_circuit.hit", target.getDisplayName()));
			} else {
				player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.no_target"));
			}
		} else if (skill.getItem() instanceof ContagionChipItem) {
			LivingEntity target = findTarget(player);

			if (target != null) {
				ContagionManager.infect(level, target);
				player.sendSystemMessage(
					Component.translatable("equipment-plus.os_skill.contagion.hit", target.getDisplayName()));
			} else {
				player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.no_target"));
			}
		} else if (skill.getItem() instanceof MemoryWipeChipItem) {
			if (findTarget(player) instanceof Mob mob) {
				MemoryWipeManager.wipe(level, mob);
				player.sendSystemMessage(
					Component.translatable("equipment-plus.os_skill.memory_wipe.hit", mob.getDisplayName()));
			} else {
				player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.no_target"));
			}
		} else {
			player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.empty"));
			return false;
		}
		return true;
	}

	/**
	 * Picks the living entity under the player's crosshair along their look direction.
	 * Mirrors the client highlight: entity and block are both picked, and the nearer
	 * one wins, so aiming at a mob's legs (with the ground behind it) still hits.
	 */
	private static LivingEntity findTarget(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().scale(RAYCAST_RANGE);
		Vec3 end = eye.add(look);

		BlockHitResult blockResult = level.clip(new ClipContext(eye, end,
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		AABB search = player.getBoundingBox().expandTowards(look).inflate(1.0D);
		List<Entity> entities = level.getEntities(player, search,
			entity -> entity instanceof LivingEntity && entity.isAlive());
		double best = Double.MAX_VALUE;
		LivingEntity hit = null;
		Vec3 hitLocation = null;

		for (Entity entity : entities) {
			AABB box = entity.getBoundingBox();

			if (box.getSize() < 0.3D) {
				box = box.inflate(0.3D);
			}

			if (box.contains(eye)) {
				return (LivingEntity) entity;
			}

			Optional<Vec3> point = box.clip(eye, end);

			if (point.isPresent()) {
				double distance = eye.distanceToSqr(point.get());

				if (distance < best) {
					best = distance;
					hit = (LivingEntity) entity;
					hitLocation = point.get();
				}
			}
		}

		if (hit != null
			&& blockResult.getType() == HitResult.Type.BLOCK
			&& hitLocation != null
			&& hitLocation.distanceToSqr(eye)
				> blockResult.getLocation().distanceToSqr(eye)) {
			return null;
		}
		return hit;
	}
}
package yuboobo.equipment.skill;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yuboobo.accessories.api.AccessoriesAPI;
import yuboobo.accessories.api.type.capability.ICuriosItemHandler;
import yuboobo.accessories.api.type.inventory.ICurioStacksHandler;
import yuboobo.accessories.api.type.inventory.IDynamicStackHandler;

import yuboobo.equipment.EquipmentPlus;
import yuboobo.equipment.item.OperatingSystemItem;

public class OSSkillManager {

	private static final double RAYCAST_RANGE = 32.0D;
	private static final float SKILL_DAMAGE = 6.0F;

	private OSSkillManager() {
	}

	/**
	 * Executes the skill in the given {@code os_skill} slot index. Returns true when a
	 * skill was actually fired.
	 */
	public static boolean execute(ServerPlayer player, int index) {
		ICuriosItemHandler inventory = AccessoriesAPI.getCuriosInventoryOrNull(player);

		if (inventory == null
			|| inventory.findFirstCurio(EquipmentPlus.OPERATING_SYSTEM).isEmpty()) {
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
		LivingEntity target = findTarget(player);

		if (target != null) {
			target.hurtServer(player.level(), player.damageSources().generic(), SKILL_DAMAGE);
			player.sendSystemMessage(
				Component.translatable("equipment-plus.os_skill.hit", target.getDisplayName()));
		} else {
			player.sendSystemMessage(Component.translatable("equipment-plus.os_skill.no_target"));
		}
		return true;
	}

	/**
	 * Picks the living entity under the player's crosshair along their look direction.
	 */
	private static LivingEntity findTarget(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().scale(RAYCAST_RANGE);
		Vec3 end = eye.add(look);

		if (level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return null;
		}

		AABB search = player.getBoundingBox().expandTowards(look).inflate(1.0D);
		List<Entity> entities = level.getEntities(player, search,
			entity -> entity instanceof LivingEntity && entity.isAlive());
		double best = Double.MAX_VALUE;
		LivingEntity hit = null;

		for (Entity entity : entities) {
			Optional<Vec3> point = entity.getBoundingBox().inflate(0.3D).clip(eye, end);

			if (point.isPresent()) {
				double distance = eye.distanceToSqr(point.get());

				if (distance < best) {
					best = distance;
					hit = (LivingEntity) entity;
				}
			}
		}
		return hit;
	}
}

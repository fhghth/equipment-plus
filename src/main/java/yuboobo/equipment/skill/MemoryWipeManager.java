package yuboobo.equipment.skill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Handles the Memory Wipe skill: a mob loses its aggro on the player for a short time.
 * Since {@code Mob.targetSelector} is not publicly accessible, a high-priority goal in
 * the mob's goal selector keeps clearing the target (and last hurt mob) every tick while
 * the wipe lasts; target goals in the target selector cannot be selected while this goal
 * runs, so the mob does not re-aggro. When the wipe expires the blocker goal is removed
 * and normal targeting resumes.
 */
public class MemoryWipeManager {

	public static final int WIPE_TICKS = 100;
	private static final int BLOCKER_PRIORITY = 1;
	private static final Map<UUID, Long> wiped = new HashMap<>();
	private static final Map<UUID, ForgetTargetGoal> blockers = new HashMap<>();

	private MemoryWipeManager() {
	}

	public static boolean isWiped(Mob mob) {
		return wiped.containsKey(mob.getUUID());
	}

	public static void wipe(ServerLevel level, Mob mob) {
		wiped.put(mob.getUUID(), level.getGameTime() + WIPE_TICKS);

		if (!blockers.containsKey(mob.getUUID())) {
			ForgetTargetGoal goal = new ForgetTargetGoal(mob);
			blockers.put(mob.getUUID(), goal);
			mob.getGoalSelector().addGoal(BLOCKER_PRIORITY, goal);
		}
	}

	public static void tick(ServerLevel level) {
		long gameTime = level.getGameTime();
		Iterator<Map.Entry<UUID, Long>> iterator = wiped.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());

			if (entity != null
				&& (entry.getValue() <= gameTime || !entity.isAlive())) {
				ForgetTargetGoal goal = blockers.remove(entry.getKey());

				if (goal != null && entity instanceof Mob mob) {
					mob.getGoalSelector().removeGoal(goal);
				}
				iterator.remove();
			}
		}
	}

	/**
	 * Runs at {@link #BLOCKER_PRIORITY} while the mob is wiped, clearing any (re-)target
	 * every tick so the mob cannot re-acquire aggro.
	 */
	static class ForgetTargetGoal extends Goal {

		private final Mob mob;

		ForgetTargetGoal(Mob mob) {
			this.mob = mob;
		}

		@Override
		public boolean canUse() {
			return isWiped(this.mob);
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
			clearTarget();
		}

		@Override
		public void tick() {
			clearTarget();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		private void clearTarget() {
			if (this.mob.getTarget() != null || this.mob.getLastHurtByMob() != null) {
				this.mob.setTarget(null);
				this.mob.setLastHurtByMob(null);
				this.mob.getNavigation().stop();
			}
		}
	}
}
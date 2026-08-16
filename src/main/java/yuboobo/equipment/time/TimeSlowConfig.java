package yuboobo.equipment.time;

import yuboobo.accessories.api.config.ConfigFile;
import yuboobo.accessories.api.config.ConfigValue;

/**
 * Configuration for the slow-motion effect, written to
 * {@code config/accessories-api/equipment-plus.json}.
 */
public final class TimeSlowConfig {

	private static final ConfigValue<Integer> SLOW_RANGE;
	private static final ConfigValue<Integer> SLOW_PERCENT;

	static {
		ConfigFile file = ConfigFile.of("equipment-plus");
		SLOW_RANGE = file.integer("slow_motion_range", 64, 8, 256);
		SLOW_PERCENT = file.integer("slow_motion_percent", 98, 50, 99);
		file.save();
	}

	private TimeSlowConfig() {
	}

	/**
	 * The radius in blocks around the activating player inside which entities
	 * are slowed down.
	 */
	public static int slowRange() {
		return SLOW_RANGE.get();
	}

	/**
	 * The speed factor applied to slowed entities, derived from the configured
	 * slow percentage (e.g. 98% slow -&gt; 0.02).
	 */
	public static double speedFactor() {
		return (100.0D - SLOW_PERCENT.get()) / 100.0D;
	}

	/**
	 * Forces the configuration declaration to load and register its handle.
	 */
	public static void init() {
	}
}

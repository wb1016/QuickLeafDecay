package dev.blueon.quickleafdecay;

import dev.blueon.quickleafdecay.FeatureControl.PersistentLeavesBehavior;
import dev.blueon.quickleafdecay.configloader.ConfigLoader;
import static dev.blueon.quickleafdecay.QuickLeafDecay.NAMESPACE;

public class Config {
	public static boolean matchLeavesTypes = false;
	public static boolean unknownLeavesOnlyMatchSelf = true;
	public static boolean matchLogsToLeaves = true;
	public static PersistentLeavesBehavior persistentLeavesBehavior = FeatureControl.PersistentLeavesBehavior.IGNORE;
	public static boolean accelerateLeavesDecay = true;
	public static int minDecayDelay = 10;
	public static int maxDecayDelay = 60;
	public static boolean updateDiagonalLeaves = true;
	public static boolean doDecayingLeavesEffects = false;

	// Saving and loading

	public static void load() {
		ConfigLoader.load(Config.class, getFileName());
	}

	public static void save() {
		ConfigLoader.save(Config.class, getFileName());
	}

	private static String getFileName() {
		return "quickleafdecay.json";
	}

}

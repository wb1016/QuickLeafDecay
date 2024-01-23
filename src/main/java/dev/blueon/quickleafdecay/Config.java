package dev.blueon.quickleafdecay;

import dev.blueon.quickleafdecay.configloader.ConfigLoader;

public class Config
{
    public static boolean matchLeavesTypes = true;
    public static boolean unknownLeavesOnlyMatchSelf = true;
    public static boolean matchLogsToLeaves = true;
    public static boolean ignorePersistentLeaves = true;
    public static boolean accelerateLeavesDecay = true;
    public static int minDecayDelay = 10;
	public static int maxDecayDelay = 60;	
	public static boolean updateDiagonalLeaves = true;
	public static boolean doDecayingLeavesEffects = false;
	public static boolean fetchTranslationUpdates = true;

    // Saving and loading

    public static void load()
    {
        ConfigLoader.load(Config.class, getFileName());
    }

    public static void save()
    {
        ConfigLoader.save(Config.class, getFileName());
    }

    private static String getFileName()
    {
        return "quickleafdecay.json";
    }

}

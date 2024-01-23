package dev.blueon.quickleafdecay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

public final class FeatureControl {
	
    public static boolean shouldMatchLeavesTypes() {
        return Config.matchLeavesTypes;
    }

    public static boolean shouldUnknownLeavesOnlyMatchSelf() {
        return Config.unknownLeavesOnlyMatchSelf;
    }

    public static boolean shouldMatchLogsToLeaves() {
        return Config.matchLogsToLeaves;
    }

    public static boolean shouldIgnorePersistentLeaves() {
        return Config.ignorePersistentLeaves;
    }

    public static boolean shouldAccelerateLeavesDecay() {
        return Config.accelerateLeavesDecay;
    }

    public static int getDecayDelay(RandomGenerator random) {
        final int minDecayDelay;
        final int maxDecayDelay;
        
        minDecayDelay = Config.minDecayDelay;
        maxDecayDelay = Config.maxDecayDelay;

        return minDecayDelay < maxDecayDelay ?
             random.range(minDecayDelay, maxDecayDelay + 1) : maxDecayDelay;
    }

    public static boolean shouldUpdateDiagonalLeaves() {
        return Config.updateDiagonalLeaves;
    }

    public static boolean shouldDoDecayingLeavesEffects() {
        return Config.doDecayingLeavesEffects;
    }

    public static boolean shouldFetchTranslationUpdates() {
        return Config.fetchTranslationUpdates;
    }

    public static boolean isMatchingLeaves(TagKey<Block> leavesTag, @NotNull BlockState state, @NotNull BlockState currentLeavesState) {
        if (state.getBlock() == currentLeavesState.getBlock()) return true;
        else if (leavesTag == null) return !shouldUnknownLeavesOnlyMatchSelf();
        else return state.isIn(leavesTag);
    }
    
    public static void init() {
		Config.load();
        Config.save();
	}

    private FeatureControl() { }
}

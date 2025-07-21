package dev.blueon.quickleafdecay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.random.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.blueon.quickleafdecay.QuickLeafDecay.DECAYS_SLOWLY;
import static dev.blueon.quickleafdecay.Util.isModLoaded;

import dev.blueon.quickleafdecay.Config;

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

	public static PersistentLeavesBehavior getPersistentLeavesBehavior() {
		return Config.persistentLeavesBehavior;
	}

	public static boolean shouldAccelerateLeavesDecay(BlockState state) {
        return !state.isIn(DECAYS_SLOWLY) && (
            Config.accelerateLeavesDecay
        );
    }

	public static int getDecayDelay(Random random) {
		final int minDecayDelay;
		final int maxDecayDelay;

		minDecayDelay = Config.minDecayDelay;
		maxDecayDelay = Config.maxDecayDelay;

		return minDecayDelay < maxDecayDelay ?
			random.nextBetweenExclusive(minDecayDelay, maxDecayDelay + 1) : maxDecayDelay;
	}

	public static boolean shouldUpdateDiagonalLeaves() {
		return Config.updateDiagonalLeaves;
	}

	public static boolean shouldDoDecayingLeavesEffects() {
		return Config.doDecayingLeavesEffects;
	}

	public static boolean isMatchingLeaves(TagKey<Block> leavesTag, @NotNull BlockState state, @NotNull BlockState currentLeavesState) {
		if (state.getBlock() == currentLeavesState.getBlock()) return true;
		else if (leavesTag == null) return !shouldUnknownLeavesOnlyMatchSelf();
		else return state.isIn(leavesTag);
	}

	public static boolean leavesMatch(@NotNull BlockState leaves1, @NotNull BlockState leaves2) {
        if (leaves1.getBlock() == leaves2.getBlock()) {
            return true;
        } else {
            @Nullable
            final TagKey<Block> leavesGroup = QuickLeafDecay.getLeavesGroup(leaves1.getBlock());
            if (leavesGroup == null) {
                return !shouldUnknownLeavesOnlyMatchSelf();
            } else {
                return leaves2.isIn(leavesGroup);
            }
        }
    }

	public static void init() {
		Config.load();
		Config.save();
	}

	private FeatureControl() {
	}

	public enum PersistentLeavesBehavior {
        IGNORE,
        NORMAL,
        MATCH_ALL
    }
}

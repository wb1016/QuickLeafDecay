package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.ticks.LevelChunkTicks;

public interface WorldChunkMixinAccessor {
    void quickleafdecay$setLeavesDecayTickScheduler(LevelChunkTicks<LeavesBlock> leavesDecayTickScheduler);
}

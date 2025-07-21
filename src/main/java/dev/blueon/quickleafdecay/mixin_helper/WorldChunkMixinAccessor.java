package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.block.LeavesBlock;
import net.minecraft.world.tick.ChunkTickScheduler;

public interface WorldChunkMixinAccessor {
    void quickleafdecay$setLeavesDecayTickScheduler(ChunkTickScheduler<LeavesBlock> leavesDecayTickScheduler);
}

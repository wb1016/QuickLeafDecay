package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.WorldTickScheduler;

public interface ServerWorldMixinAccessor {
    WorldTickScheduler<LeavesBlock> quickleafdecay$getLeavesDecayTickScheduler();

    void quickleafdecay$scheduleLeavesDecayTick(BlockPos pos, LeavesBlock leavesBlock, int delay);
}

package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTicks;

public interface ServerWorldMixinAccessor {
    LevelTicks<LeavesBlock> quickleafdecay$getLeavesDecayTickScheduler();

    void quickleafdecay$scheduleLeavesDecayTick(BlockPos pos, LeavesBlock leavesBlock, int delay);
}

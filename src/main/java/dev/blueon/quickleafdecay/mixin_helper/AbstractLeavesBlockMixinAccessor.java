package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public interface AbstractLeavesBlockMixinAccessor {
    void quickleafdecay$tryDecaying(ServerLevel world, BlockPos pos, BlockState state, RandomSource random);
}

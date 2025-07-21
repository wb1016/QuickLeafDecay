package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public interface AbstractLeavesBlockMixinAccessor {
    void quickleafdecay$tryDecaying(ServerWorld world, BlockPos pos, BlockState state, Random random);
}

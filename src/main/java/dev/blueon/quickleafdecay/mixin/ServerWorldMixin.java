package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.AbstractLeavesBlockMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
abstract class ServerWorldMixin extends World implements StructureWorldAccess, ServerWorldMixinAccessor {
    @Unique
    private static final String LEAVES_DECAY_TICKS_PROFILER_LOCATION = QuickLeafDecay.NAMESPACE + ":leavesDecayTicks";

    private ServerWorldMixin() {
        //noinspection DataFlowIssue
        super(null, null, null, null, false, false, 0, 0);
        throw new IllegalStateException("Dummy constructor called!");
    }

    @Shadow
    public abstract boolean isChunkLoaded(long l);

    @Unique
    private WorldTickScheduler<LeavesBlock> leavesDecayTickScheduler;

    @Override
    public WorldTickScheduler<LeavesBlock> quickleafdecay$getLeavesDecayTickScheduler() {
        return this.leavesDecayTickScheduler;
    }

    @Override
    public void quickleafdecay$scheduleLeavesDecayTick(BlockPos pos, LeavesBlock leavesBlock, int delay) {
        this.leavesDecayTickScheduler.scheduleTick(new OrderedTick<>(
            leavesBlock, pos,
            this.getLevelProperties().getTime() + (long) delay,
            this.getTickOrder()
        ));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initFields(CallbackInfo ci) {
        this.leavesDecayTickScheduler = new WorldTickScheduler<>(this::isChunkLoaded);
    }

    @Inject(
        method = "tick", require = 1, allow = 1,
        at = @At(
            value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/tick/WorldTickScheduler;tick(JILjava/util/function/BiConsumer;)V"
        ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/server/world/ServerWorld;blockTickScheduler:" +
                    "Lnet/minecraft/world/tick/WorldTickScheduler;"
            ),
            to = @At(
                value = "FIELD",
                target = "Lnet/minecraft/server/world/ServerWorld;fluidTickScheduler:" +
                    "Lnet/minecraft/world/tick/WorldTickScheduler;"
            )
        )
    )
    private void tickLeavesDecayScheduler(CallbackInfo ci, @Local Profiler profiler) {
        profiler.swap(LEAVES_DECAY_TICKS_PROFILER_LOCATION);
        this.leavesDecayTickScheduler.tick(this.getTime(), 65536, this::tryDecaying);
    }

    @Unique
    private void tryDecaying(BlockPos pos, LeavesBlock leavesBlock) {
        final BlockState state = this.getBlockState(pos);
        if (state.isOf(leavesBlock)) {
            ((AbstractLeavesBlockMixinAccessor) leavesBlock).quickleafdecay$tryDecaying(
                (ServerWorld)(Object) this, pos, state, this.random
            );
        }
    }
}

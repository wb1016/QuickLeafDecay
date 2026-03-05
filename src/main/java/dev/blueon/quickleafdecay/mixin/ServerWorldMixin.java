package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.AbstractLeavesBlockMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.LevelTicks;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerWorldMixin extends Level implements ServerLevelAccessor, ServerWorldMixinAccessor {
    @Unique
    private static final String LEAVES_DECAY_TICKS_PROFILER_LOCATION = QuickLeafDecay.NAMESPACE + ":leavesDecayTicks";

    private ServerWorldMixin() {
        //noinspection DataFlowIssue
        super(null, null, null, null, false, false, 0, 0);
        throw new IllegalStateException("Dummy constructor called!");
    }

    @Shadow
    public abstract boolean isPositionTickingWithEntitiesLoaded(long l);

    @Unique
    private LevelTicks<LeavesBlock> leavesDecayTickScheduler;

    @Override
    public LevelTicks<LeavesBlock> quickleafdecay$getLeavesDecayTickScheduler() {
        return this.leavesDecayTickScheduler;
    }

    @Override
    public void quickleafdecay$scheduleLeavesDecayTick(BlockPos pos, LeavesBlock leavesBlock, int delay) {
        this.leavesDecayTickScheduler.schedule(new ScheduledTick<>(
            leavesBlock, pos,
            this.getGameTime() + (long) delay,
            this.nextSubTickCount()
        ));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initFields(CallbackInfo ci) {
        this.leavesDecayTickScheduler = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded);
    }

    @Inject(
        method = "tick", require = 1, allow = 1,
        at = @At(
            value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V"
        ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lnet/minecraft/server/level/ServerLevel;blockTicks:" +
                    "Lnet/minecraft/world/ticks/LevelTicks;"
            ),
            to = @At(
                value = "FIELD",
                target = "Lnet/minecraft/server/level/ServerLevel;fluidTicks:" +
                    "Lnet/minecraft/world/ticks/LevelTicks;"
            )
        )
    )
    private void tickLeavesDecayScheduler(CallbackInfo ci, @Local ProfilerFiller profiler) {
        profiler.popPush(LEAVES_DECAY_TICKS_PROFILER_LOCATION);
        this.leavesDecayTickScheduler.tick(this.getGameTime(), 65536, this::tryDecaying);
    }

    @Unique
    private void tryDecaying(BlockPos pos, LeavesBlock leavesBlock) {
        final BlockState state = this.getBlockState(pos);
        if (state.is(leavesBlock)) {
            ((AbstractLeavesBlockMixinAccessor) leavesBlock).quickleafdecay$tryDecaying(
                (ServerLevel)(Object) this, pos, state, this.getRandom()
            );
        }
    }
}

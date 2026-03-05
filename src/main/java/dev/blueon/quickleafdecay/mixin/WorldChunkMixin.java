package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.WorldChunkMixinAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.LevelChunkTicks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class WorldChunkMixin extends ChunkAccess implements WorldChunkMixinAccessor {
    @Unique
    private LevelChunkTicks<LeavesBlock> leavesDecayTickScheduler;

    private WorldChunkMixin() {
        //noinspection DataFlowIssue
        super(null, null, null, null, 0, null, null);
        throw new IllegalStateException("Dummy constructor called!");
    }

    @Override
    public void quickleafdecay$setLeavesDecayTickScheduler(
        LevelChunkTicks<LeavesBlock> leavesDecayTickScheduler
    ) {
        this.leavesDecayTickScheduler = leavesDecayTickScheduler;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;" +
            "Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/ticks/LevelChunkTicks;" +
            "Lnet/minecraft/world/ticks/LevelChunkTicks;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;" +
            "Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;Lnet/minecraft/world/level/levelgen/blending/BlendingData;)V",
        at = @At("TAIL")
    )
    private void initFields(CallbackInfo ci) {
        this.leavesDecayTickScheduler = new LevelChunkTicks<>();
    }

    @Inject(method = "registerTickContainerInLevel", at = @At("TAIL"))
    private void addLeavesDecayTickScheduler(ServerLevel level, CallbackInfo ci) {
        ((ServerWorldMixinAccessor) level).quickleafdecay$getLeavesDecayTickScheduler()
            .addContainer(this.chunkPos, this.leavesDecayTickScheduler);
    }

    @Inject(method = "unregisterTickContainerFromLevel", at = @At("TAIL"))
    private void removeLeavesDecayTickScheduler(ServerLevel level, CallbackInfo ci) {
        ((ServerWorldMixinAccessor) level).quickleafdecay$getLeavesDecayTickScheduler()
            .removeContainer(this.chunkPos);
    }

    @Inject(method = "getTicksForSerialization", at = @At("RETURN"))
    private void packLeavesDecayTicks(long time, CallbackInfoReturnable<ChunkAccess.PackedTicks> cir) {
        ((PackedTicksMixinAccessor) (Object) cir.getReturnValue()).quickleafdecay$setLeavesDecayTicks(
            this.leavesDecayTickScheduler.pack(time)
        );
    }
}

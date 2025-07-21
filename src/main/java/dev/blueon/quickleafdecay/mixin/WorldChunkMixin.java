package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.WorldChunkMixinAccessor;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.ChunkTickScheduler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
abstract class WorldChunkMixin extends Chunk implements WorldChunkMixinAccessor {
    @Unique
    private ChunkTickScheduler<LeavesBlock> leavesDecayTickScheduler;

    private WorldChunkMixin() {
        //noinspection DataFlowIssue
        super(null, null, null, null, 0, null, null);
        throw new IllegalStateException("Dummy constructor called!");
    }

    @Override
    public void quickleafdecay$setLeavesDecayTickScheduler(
        ChunkTickScheduler<LeavesBlock> leavesDecayTickScheduler
    ) {
        this.leavesDecayTickScheduler = leavesDecayTickScheduler;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;" +
            "Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;" +
            "Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;" +
            "Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V",
        at = @At("TAIL")
    )
    private void initFields(CallbackInfo ci) {
        this.leavesDecayTickScheduler = new ChunkTickScheduler<>();
    }

    @Inject(method = "addChunkTickSchedulers", at = @At("TAIL"))
    private void scheduleInitialLeavesDecayTicks(ServerWorld world, CallbackInfo ci) {
        long time = world.getTime();
        this.leavesDecayTickScheduler.disable(time);
    }

    @Inject(method = "addChunkTickSchedulers", at = @At("TAIL"))
    private void addLeavesDecayTickScheduler(ServerWorld world, CallbackInfo ci) {
        ((ServerWorldMixinAccessor) world).quickleafdecay$getLeavesDecayTickScheduler()
            .addChunkTickScheduler(this.pos, this.leavesDecayTickScheduler);
    }

    @Inject(method = "removeChunkTickSchedulers", at = @At("TAIL"))
    private void removeLeavesDecayTickScheduler(ServerWorld world, CallbackInfo ci) {
        ((ServerWorldMixinAccessor) world).quickleafdecay$getLeavesDecayTickScheduler()
            .removeChunkTickScheduler(this.pos);
    }

    @Inject(method = "getTickSchedulers", at = @At("RETURN"))
    private void packLeavesDecayTicks(long time, CallbackInfoReturnable<Chunk.TickSchedulers> cir) {
        ((PackedTicksMixinAccessor) (Object) cir.getReturnValue()).quickleafdecay$setLeavesDecayTicks(
            this.leavesDecayTickScheduler.collectTicks(time)
        );
    }
}

package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.WorldChunkMixinAccessor;
import net.minecraft.block.LeavesBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.Tick;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SerializedChunk.class)
abstract class ChunkSerializerMixin {
    @Shadow @Final private Chunk.TickSchedulers packedTicks;
    @Unique
    private static final String LEAVES_DECAY_SCHEDULER_KEY = QuickLeafDecay.NAMESPACE + ":leaves_decay_ticks";

    @Unique
    private static final Codec<LeavesBlock> LEAVES_CODEC = Registries.BLOCK.getCodec().flatXmap(
        block -> {
            if (block instanceof LeavesBlock leavesBlock) {
                return DataResult.success(leavesBlock);
            } else {
                return DataResult.error(() -> "non-leaves block: " + Registries.BLOCK.getId(block));
            }
        },
        DataResult::success
    );

    @Unique
    private static final Codec<List<Tick<LeavesBlock>>> LEAVES_TICK_CODEC =
        Tick.createCodec(LEAVES_CODEC).listOf();

    @ModifyExpressionValue(
        method = "fromNbt",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/world/chunk/Chunk$TickSchedulers;"
        )
    )
    private static Chunk.TickSchedulers deserializeAndPackLeavesDecayTicks(
        Chunk.TickSchedulers originalPackedTicks,
        HeightLimitView world, PalettesFactory palettesFactory, NbtCompound nbt,
        @Local ChunkPos chunkPos
    ) {
        ((PackedTicksMixinAccessor) (Object) originalPackedTicks)
            .quickleafdecay$setLeavesDecayTicks(
                nbt.get(LEAVES_DECAY_SCHEDULER_KEY, LEAVES_TICK_CODEC).orElse(List.of())
            );

        return originalPackedTicks;
    }

    @ModifyExpressionValue(
        method = "convert",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/world/chunk/WorldChunk;"
        )
    )
    private WorldChunk unpackTicksAndSetLeavesDecayScheduler(WorldChunk original) {
        ((WorldChunkMixinAccessor) original).quickleafdecay$setLeavesDecayTickScheduler(
            new ChunkTickScheduler<>(
                ((PackedTicksMixinAccessor) (Object) this.packedTicks).quickleafdecay$getLeavesDecayTicks()
            )
        );

        return original;
    }

    @Inject(
        method = "serializeTicks",
        at = @At("TAIL")
    )
    private static void unpackAndSerializeLeavesDecayTicks(NbtCompound nbt, Chunk.TickSchedulers packedTicks, CallbackInfo ci){
        final List<Tick<LeavesBlock>> leavesDecayTicks =
            ((PackedTicksMixinAccessor) (Object) packedTicks).quickleafdecay$getLeavesDecayTicks();
        // empty for non-WorldChunk Chunks
        if (!leavesDecayTicks.isEmpty()) {
            nbt.put(LEAVES_DECAY_SCHEDULER_KEY, LEAVES_TICK_CODEC, leavesDecayTicks);
        }
    }
}

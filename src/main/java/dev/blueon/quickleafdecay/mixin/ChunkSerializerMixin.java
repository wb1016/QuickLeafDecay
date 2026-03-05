package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.WorldChunkMixinAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SerializableChunkData.class)
abstract class ChunkSerializerMixin {
    @Unique
    private static final String LEAVES_DECAY_SCHEDULER_KEY = QuickLeafDecay.NAMESPACE + ":leaves_decay_ticks";

    @Unique
    private static final Codec<LeavesBlock> LEAVES_CODEC = BuiltInRegistries.BLOCK.byNameCodec().flatXmap(
        block -> {
            if (block instanceof LeavesBlock leavesBlock) {
                return DataResult.success(leavesBlock);
            } else {
                return DataResult.error(() -> "non-leaves block: " + BuiltInRegistries.BLOCK.getKey(block));
            }
        },
        DataResult::success
    );

    @Unique
    private static final Codec<List<SavedTick<LeavesBlock>>> LEAVES_TICK_CODEC =
        SavedTick.codec(LEAVES_CODEC).listOf();

    @Shadow @Final private ChunkAccess.PackedTicks packedTicks;

    @ModifyExpressionValue(
        method = "parse",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess$PackedTicks;"
        )
    )
    private static ChunkAccess.PackedTicks deserializeAndPackLeavesDecayTicks(
        ChunkAccess.PackedTicks originalPackedTicks,
        LevelHeightAccessor world, PalettedContainerFactory palettesFactory, CompoundTag nbt,
        @Local ChunkPos chunkPos
    ) {
        ((PackedTicksMixinAccessor) (Object) originalPackedTicks)
            .quickleafdecay$setLeavesDecayTicks(
                nbt.read(LEAVES_DECAY_SCHEDULER_KEY, LEAVES_TICK_CODEC).orElse(List.of())
            );

        return originalPackedTicks;
    }

    @ModifyExpressionValue(
        method = "read",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;"
        )
    )
    private LevelChunk unpackTicksAndSetLeavesDecayScheduler(LevelChunk chunk) {
        ((WorldChunkMixinAccessor) chunk).quickleafdecay$setLeavesDecayTickScheduler(
            new LevelChunkTicks<>(
                ((PackedTicksMixinAccessor) (Object) this.packedTicks).quickleafdecay$getLeavesDecayTicks()
            )
        );
        return chunk;
    }

    @WrapOperation(
        method = "write",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/storage/SerializableChunkData;saveTicks(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/chunk/ChunkAccess$PackedTicks;)V"
        )
    )
    private void wrapSaveTicksAndSerializeLeavesDecayTicks(
        CompoundTag nbt, 
        ChunkAccess.PackedTicks packedTicks,
        Operation<Void> original
    ) {
        original.call(nbt, packedTicks);
        
        final List<SavedTick<LeavesBlock>> leavesDecayTicks =
            ((PackedTicksMixinAccessor) (Object) packedTicks).quickleafdecay$getLeavesDecayTicks();
        if (!leavesDecayTicks.isEmpty()) {
            nbt.store(LEAVES_DECAY_SCHEDULER_KEY, LEAVES_TICK_CODEC, leavesDecayTicks);
        }
    }
}

package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.ticks.SavedTick;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChunkAccess.PackedTicks.class)
abstract class PackedTicksMixin implements PackedTicksMixinAccessor {
    @Unique
    private List<SavedTick<LeavesBlock>> leavesDecayTicks;

    @Override
    public List<SavedTick<LeavesBlock>> quickleafdecay$getLeavesDecayTicks() {
        return this.leavesDecayTicks;
    }

    @Override
    public void quickleafdecay$setLeavesDecayTicks(List<SavedTick<LeavesBlock>> leavesDecayTicks) {
        this.leavesDecayTicks = leavesDecayTicks;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initFields(CallbackInfo ci) {
        // initialize value so non-LevelChunks don't throw NPEs
        this.leavesDecayTicks = new ArrayList<>();
    }
}

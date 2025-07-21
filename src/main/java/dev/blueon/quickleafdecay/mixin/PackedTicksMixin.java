package dev.blueon.quickleafdecay.mixin;

import dev.blueon.quickleafdecay.mixin_helper.PackedTicksMixinAccessor;
import net.minecraft.block.LeavesBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.tick.Tick;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Chunk.TickSchedulers.class)
abstract class PackedTicksMixin implements PackedTicksMixinAccessor {
    @Unique
    private List<Tick<LeavesBlock>> leavesDecayTicks;

    @Override
    public List<Tick<LeavesBlock>> quickleafdecay$getLeavesDecayTicks() {
        return this.leavesDecayTicks;
    }

    @Override
    public void quickleafdecay$setLeavesDecayTicks(List<Tick<LeavesBlock>> leavesDecayTicks) {
        this.leavesDecayTicks = leavesDecayTicks;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initFields(CallbackInfo ci) {
        // initialize value so non-WorldChunks don't throw NPEs
        this.leavesDecayTicks = new ArrayList<>();
    }
}

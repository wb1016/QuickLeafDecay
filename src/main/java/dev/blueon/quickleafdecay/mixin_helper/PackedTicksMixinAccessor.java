package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.block.LeavesBlock;
import net.minecraft.world.tick.Tick;

import java.util.List;

public interface PackedTicksMixinAccessor {
    List<Tick<LeavesBlock>> quickleafdecay$getLeavesDecayTicks();

    void quickleafdecay$setLeavesDecayTicks(List<Tick<LeavesBlock>> ticks);
}

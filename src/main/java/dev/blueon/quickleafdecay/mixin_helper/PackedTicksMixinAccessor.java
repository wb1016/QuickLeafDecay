package dev.blueon.quickleafdecay.mixin_helper;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.ticks.SavedTick;

import java.util.List;

public interface PackedTicksMixinAccessor {
    List<SavedTick<LeavesBlock>> quickleafdecay$getLeavesDecayTicks();

    void quickleafdecay$setLeavesDecayTicks(List<SavedTick<LeavesBlock>> ticks);
}

package dev.blueon.quickleafdecay.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;

import dev.blueon.quickleafdecay.FeatureControl.PersistentLeavesBehavior;
import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.AbstractLeavesBlockMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import static dev.blueon.quickleafdecay.FeatureControl.getDecayDelay;
import static dev.blueon.quickleafdecay.FeatureControl.leavesMatch;
import static dev.blueon.quickleafdecay.FeatureControl.shouldAccelerateLeavesDecay;
import static dev.blueon.quickleafdecay.FeatureControl.shouldDoDecayingLeavesEffects;
import static dev.blueon.quickleafdecay.FeatureControl.getPersistentLeavesBehavior;
import static dev.blueon.quickleafdecay.FeatureControl.shouldMatchLeavesTypes;
import static dev.blueon.quickleafdecay.FeatureControl.shouldMatchLogsToLeaves;
import static dev.blueon.quickleafdecay.FeatureControl.shouldUpdateDiagonalLeaves;
import static dev.blueon.quickleafdecay.QuickLeafDecay.LOGS_WITHOUT_LEAVES;

@Mixin(LeavesBlock.class)
abstract class AbstractLeavesBlockMixin extends Block implements AbstractLeavesBlockMixinAccessor {
	@Unique
	@NotNull
	private static final ThreadLocal<Optional<BlockState>> currentLeaves =
		ThreadLocal.withInitial(Optional::empty);

	@Shadow
	@Final
	public static BooleanProperty PERSISTENT;

	@Shadow
	protected abstract boolean decaying(BlockState state);

	@Shadow
	protected abstract void spawnFallingLeavesParticle(net.minecraft.world.level.Level level, BlockPos pos, RandomSource random);

	private AbstractLeavesBlockMixin() {
        //noinspection DataFlowIssue
        super(null);
		throw new IllegalStateException("Dummy constructor called!");
	}

	@Override
	public void quickleafdecay$tryDecaying(
		ServerLevel world, BlockPos pos, BlockState state, RandomSource random
	) {
		if (shouldAccelerateLeavesDecay(state) && this.decaying(state)) {
			dropResources(state, world, pos);
			world.removeBlock(pos, false);
			this.trySpawningDecayEffects(state, world, pos);

			if (shouldUpdateDiagonalLeaves()) {
				getDiagonalPositions(pos).forEach(diagonalPos -> {
					final BlockState diagonalState = world.getBlockState(diagonalPos);
					if (
						diagonalState != null &&
						diagonalState.getBlock() instanceof UntintedParticleLeavesBlock diagonalLeaves &&
						leavesMatch(diagonalState, this.defaultBlockState())
					) {
						world.scheduleTick(diagonalPos, diagonalLeaves, 0);
					}
				});
			}
		}
	}

	@Inject(method = "updateShape", at = @At(value = "HEAD"))
	private void captureNeighborBlock(
		BlockState state, LevelReader world, ScheduledTickAccess tickSchedulerAccess, BlockPos pos, 
		Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random,
		CallbackInfoReturnable<BlockState> cir
	) {
		if (shouldMatchLeavesTypes()) {
            QuickLeafDecay.updateLeavesGroups(this);
        }

		currentLeaves.set(Optional.of(state));
	}

	@Inject(method = "updateShape", at = @At(value = "TAIL"))
	private void resetCapturedNeighborBlock(CallbackInfoReturnable<BlockState> cir) {
		currentLeaves.remove();
	}

	@WrapMethod(method = "updateDistance")
	private static BlockState captureUpdatingBlock(
		BlockState state, LevelAccessor world, BlockPos pos,
		Operation<BlockState> original
	) {
		if (shouldMatchLeavesTypes()) {
			QuickLeafDecay.updateLeavesGroups(state.getBlock());
		}

		currentLeaves.set(Optional.of(state));

		final BlockState newState = original.call(state, world, pos);

		currentLeaves.remove();

		return newState;
	}

	@ModifyArg(
		method = "updateDistance",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/LeavesBlock;getDistanceAt(Lnet/minecraft/world/level/block/state/BlockState;)I"
		)
	)
	private static BlockState checkBlockState(BlockState state) {
		if (shouldMatchLogsToLeaves()) {
			QuickLeafDecay.updateTreeTypes(state);
		}

		return state;
	}

	// If a tree tag is found, match it. Otherwise, match all logs like vanilla
	@Redirect(
		method = "getOptionalDistanceAt",
		at = @At(
			value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/tags/BlockTags;PREVENTS_NEARBY_LEAF_DECAY:Lnet/minecraft/tags/TagKey;"
			)
		)
	)
	private static boolean tryMatchLog(BlockState state, TagKey<Block> tagKey) {
		if (shouldMatchLogsToLeaves()) {
			@Nullable
			final BlockState leaves = currentLeaves.get().orElse(null);
			if (
				getPersistentLeavesBehavior() != PersistentLeavesBehavior.MATCH_ALL
					|| leaves == null
					|| !leaves.getValue(PERSISTENT)
			) {
				if (state.is(LOGS_WITHOUT_LEAVES)) {
					return false;
				}

				if (leaves != null) {
					final Block block = state.getBlock();
					final TagKey<Block> treeType = QuickLeafDecay.getTreeType(block);
					if (
						treeType != null
					) {
						return leaves.is(treeType);
					}
				}
			}
		}

		return state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY);
	}

	@WrapOperation(
		method = "getOptionalDistanceAt",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;hasProperty(Lnet/minecraft/world/level/block/state/properties/Property;)Z"
		)
	)
	private static boolean matchLeaves(
		BlockState otherLeavesState, Property<?> property, Operation<Boolean> original
	) {
		if (
			!original.call(otherLeavesState, property) ||
			currentLeaves.get().isEmpty()
		) {
			return false;
		}

		final BlockState currentLeavesState = currentLeaves.get().get();

		final PersistentLeavesBehavior persistentBehavior = getPersistentLeavesBehavior();
		if (persistentBehavior != PersistentLeavesBehavior.NORMAL) {
			final Boolean currentPersistent = currentLeavesState.getValue(PERSISTENT);
			final Boolean otherPersistent = otherLeavesState.getOptionalValue(PERSISTENT).orElse(false);
			// non-persistent leaves only care about other non-persistent leaves,
			//   persistent leaves care about BOTH non/persistent leaves
			if (
				persistentBehavior == PersistentLeavesBehavior.IGNORE &&
					(!currentPersistent && otherPersistent)
			) {
				return false;
			} else if (
				persistentBehavior == PersistentLeavesBehavior.MATCH_ALL &&
					(currentPersistent || otherPersistent)
			) {
				return true;
			}
		}

		if (shouldMatchLeavesTypes()) {
            return leavesMatch(otherLeavesState, currentLeavesState);
		}

		return true;
	}

    @WrapOperation(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;" +
				"Lnet/minecraft/world/level/block/state/BlockState;I)Z"
		)
	)
	private boolean tryAcceleratingDecay(
		ServerLevel world, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original,
		BlockState oldState, ServerLevel duplicate1, BlockPos duplicate2, RandomSource random
	) {
		final boolean originalReturn = original.call(world, pos, newState, flags);

		// checking that the old state cannot decay is important for trees that generate with
		// leaves with incorrect distances
		if (this.decaying(newState) && !this.decaying(oldState)) {
			((ServerWorldMixinAccessor) world).quickleafdecay$scheduleLeavesDecayTick(
				pos, (LeavesBlock)(Object) this, getDecayDelay(random)
			);
		}

		return originalReturn;
	}

	@Inject(
		method = "randomTick",
		at = @At(
			value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
		)
	)
	private void trySpawningDecayEffects(
		BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci
	) {
		this.trySpawningDecayEffects(state, world, pos);
	}

	@Unique
	private static Collection<BlockPos> getDiagonalPositions(BlockPos pos) {
		final Collection<BlockPos> diagonalPositions = new LinkedList<>();
		for (final Direction direction : Direction.values()) {
			if (direction.getAxis().isHorizontal()) {
				diagonalPositions.add(pos.relative(direction).relative(direction.getClockWise()));
			} else {
				final BlockPos vOffsetPos = pos.relative(direction);
				Direction.Plane.HORIZONTAL.stream().forEach(horizontal ->
					diagonalPositions.add(vOffsetPos.relative(direction).relative(horizontal.getClockWise()))
				);
			}
		}

		return diagonalPositions;
	}

	@Unique
	private void trySpawningDecayEffects(BlockState state, ServerLevel world, BlockPos pos) {
		if (shouldDoDecayingLeavesEffects()) {
			this.spawnFallingLeavesParticle(world, pos, world.getRandom());
		}
	}
}

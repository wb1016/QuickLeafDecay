package dev.blueon.quickleafdecay.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import dev.blueon.quickleafdecay.FeatureControl.PersistentLeavesBehavior;
import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.mixin_helper.AbstractLeavesBlockMixinAccessor;
import dev.blueon.quickleafdecay.mixin_helper.ServerWorldMixinAccessor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.UntintedParticleLeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
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
	protected abstract boolean shouldDecay(BlockState state);

	private AbstractLeavesBlockMixin() {
        //noinspection DataFlowIssue
        super(null);
		throw new IllegalStateException("Dummy constructor called!");
	}

	@Override
	public void quickleafdecay$tryDecaying(
		ServerWorld world, BlockPos pos, BlockState state, Random random
	) {
		if (shouldAccelerateLeavesDecay(state) && this.shouldDecay(state)) {
			dropStacks(state, world, pos);
			world.removeBlock(pos, false);
			this.trySpawningDecayEffects(state, world, pos);

			if (shouldUpdateDiagonalLeaves()) {
				getDiagonalPositions(pos).forEach(diagonalPos -> {
					final BlockState diagonalState = world.getBlockState(diagonalPos);
					if (
						diagonalState != null &&
						diagonalState.getBlock() instanceof UntintedParticleLeavesBlock diagonalLeaves &&
						leavesMatch(diagonalState, this.getDefaultState())
					) {
						world.scheduleBlockTick(diagonalPos, diagonalLeaves, 0);
					}
				});
			}
		}
	}

	@Inject(method = "getStateForNeighborUpdate",at = @At(value = "HEAD"))
	private void captureNeighborBlock(
		BlockState state, WorldView world, ScheduledTickView tickSchedulerAccess, BlockPos pos, Direction direction,
		BlockPos neighborPos, BlockState neighborState, Random random,
		CallbackInfoReturnable<BlockState> cir
	) {
		if (shouldMatchLeavesTypes()) {
            QuickLeafDecay.updateLeavesGroups(this);
        }

		currentLeaves.set(Optional.of(state));
	}

	@Inject(method = "getStateForNeighborUpdate",at = @At(value = "TAIL"))
	private void resetCapturedNeighborBlock(CallbackInfoReturnable<BlockState> cir) {
		currentLeaves.remove();
	}

	@WrapMethod(method = "updateDistanceFromLogs")
	private static BlockState captureUpdatingBlock(
		BlockState state, WorldAccess world, BlockPos pos,
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
		method = "updateDistanceFromLogs",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/LeavesBlock;getDistanceFromLog(Lnet/minecraft/block/BlockState;)I"
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
		method = "getOptionalDistanceFromLog",
		at = @At(
			value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/block/BlockState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/registry/tag/BlockTags;LOGS:Lnet/minecraft/registry/tag/TagKey;"
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
					|| !leaves.get(PERSISTENT)
			) {
				if (state.isIn(LOGS_WITHOUT_LEAVES)) {
					return false;
				}

				if (leaves != null) {
					final Block block = state.getBlock();
					final TagKey<Block> treeType = QuickLeafDecay.getTreeType(block);
					if (
						treeType != null &&
							Registries.BLOCK.getOptional(treeType).isPresent()
					) {
						return leaves.isIn(treeType);
					}
				}
			}
		}

		return state.isIn(BlockTags.LOGS);
	}

	@WrapOperation(
		method = "getOptionalDistanceFromLog",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/BlockState;contains(Lnet/minecraft/state/property/Property;)Z"
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
			final Boolean currentPersistent = currentLeavesState.get(PERSISTENT);
			final Boolean otherPersistent = otherLeavesState.getOrEmpty(PERSISTENT).orElse(false);
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
		method = "scheduledTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;" +
				"Lnet/minecraft/block/BlockState;I)Z"
		)
	)
	private boolean tryAcceleratingDecay(
		ServerWorld world, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original,
		BlockState oldState, ServerWorld duplicate1, BlockPos duplicate2, Random random
	) {
		final boolean originalReturn = original.call(world, pos, newState, flags);

		// checking that the old state cannot decay is important for trees that generate with
		// leaves with incorrect distances
		if (this.shouldDecay(newState) && !this.shouldDecay(oldState)) {
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
			target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"
		)
	)
	private void trySpawningDecayEffects(
		BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci
	) {
		this.trySpawningDecayEffects(state, world, pos);
	}

	@Unique
	private static Collection<BlockPos> getDiagonalPositions(BlockPos pos) {
		final Collection<BlockPos> diagonalPositions = new LinkedList<>();
		for (final Direction direction : Direction.values()) {
			if (direction.getHorizontalQuarterTurns() >= 0) {
				diagonalPositions.add(pos.offset(direction).offset(direction.rotateYClockwise()));
			} else {
				final BlockPos vOffsetPos = pos.offset(direction);
				Direction.Type.HORIZONTAL.stream().forEach(horizontal ->
					diagonalPositions.add(vOffsetPos.offset(direction).offset(horizontal.rotateYClockwise()))
				);
			}
		}

		return diagonalPositions;
	}

	@Unique
	private void trySpawningDecayEffects(BlockState state, ServerWorld world, BlockPos pos) {
		if (shouldDoDecayingLeavesEffects()) {
			this.spawnBreakParticles(world, null, pos, state);
		}
	}
}

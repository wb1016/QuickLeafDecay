package dev.blueon.quickleafdecay.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.blueon.quickleafdecay.QuickLeafDecay;
import dev.blueon.quickleafdecay.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import static dev.blueon.quickleafdecay.FeatureControl.*;
import static dev.blueon.quickleafdecay.QuickLeafDecay.LOGS_WITHOUT_LEAVES;

@Mixin(LeavesBlock.class)
abstract class LeavesBlockMixin extends Block {
	@Unique
	@NotNull
	private static final ThreadLocal<Optional<BlockState>> currentLeaves =
									ThreadLocal.withInitial(Optional::empty);

	@Shadow
	@Final
	public static IntProperty DISTANCE;
	@Shadow
	@Final
	public static BooleanProperty PERSISTENT;

	private LeavesBlockMixin(Settings settings) {
		super(settings);
		throw new IllegalStateException("MixinLeavesBlock's dummy constructor called!");
	}

	@Inject(method = "getStateForNeighborUpdate", at = @At(value = "HEAD"))
	private void captureNeighborBlock(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
		if (shouldMatchLeavesTypes()) QuickLeafDecay.updateLeavesTags(this);
		currentLeaves.set(Optional.of(state));
	}

	@Inject(method = "getStateForNeighborUpdate", at = @At(value = "TAIL"))
	private void resetCapturedNeighborBlock(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
		currentLeaves.remove();
	}

	@Inject(method = "updateDistanceFromLogs", at = @At(value = "HEAD"))
	private static void captureUpdatingBlock(BlockState state, WorldAccess world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (shouldMatchLeavesTypes()) QuickLeafDecay.updateLeavesTags(state.getBlock());
		currentLeaves.set(Optional.of(state));
	}

	@Inject(method = "updateDistanceFromLogs", at = @At(value = "TAIL"))
	private static void resetCapturedUpdatingBlock(BlockState state, WorldAccess world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		currentLeaves.remove();
	}

	@ModifyArgs(method = "updateDistanceFromLogs", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;getDistanceFromLog(Lnet/minecraft/block/BlockState;)I"))
	private static void checkBlockState(Args args, BlockState leavesState, WorldAccess world, BlockPos pos) {
		if (shouldMatchLogsToLeaves()) QuickLeafDecay.updateLogLeavesTags(((BlockState) args.get(0)).getBlock());
	}

	// If a log_leaves tag is found, match it. Otherwise, match all logs like vanilla
	@Redirect(
									method = "getOptionalDistanceFromLog",
									at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/block/BlockState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"),
									slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/registry/tag/BlockTags;LOGS:Lnet/minecraft/registry/tag/TagKey;"))
	)
	private static boolean tryMatchLog(BlockState state, TagKey<Block> tagKey) {
		if (shouldMatchLogsToLeaves()) {
			if (state.isIn(LOGS_WITHOUT_LEAVES)) return false;
			if (currentLeaves.get().isPresent()) {
				final Block block = state.getBlock();
				TagKey<Block> logLeavesTag = QuickLeafDecay.getLeavesForLog(block);
				if (
												logLeavesTag != null &&
																				Registries.BLOCK.getTag(logLeavesTag).isPresent()
				) return currentLeaves.get().get().isIn(logLeavesTag);
			}
		}

		return state.isIn(BlockTags.LOGS);
	}

	@ModifyExpressionValue(method = "getOptionalDistanceFromLog", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;contains(Lnet/minecraft/state/property/Property;)Z"))
	private static boolean matchLeaves(boolean original, BlockState state) {
		if (!original) return false;
		if (currentLeaves.get().isEmpty()) return true;
		final BlockState currentLeavesState = currentLeaves.get().get();

		if (shouldIgnorePersistentLeaves()) {
			// non-persistent only care about other non-persistent,
			//   persistent care about BOTH non/persistent
			if (!currentLeavesState.get(PERSISTENT)) {
				final Optional<Boolean> optOtherPersistent = state.getOrEmpty(PERSISTENT);
				if (optOtherPersistent.isPresent()) {
					if (optOtherPersistent.get()) return false;
				}
			}
		}

		if (state != null && shouldMatchLeavesTypes()) {
			TagKey<Block> leavesTag = QuickLeafDecay.getLeavesTag(currentLeavesState.getBlock());
			return isMatchingLeaves(leavesTag, state, currentLeavesState);
		}

		return true;
	}

	@Inject(method = "scheduledTick", at = @At("TAIL"))
	private void postScheduledTick(BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random, CallbackInfo ci) {
		if (!state.get(PERSISTENT) && shouldAccelerateLeavesDecay()) {
			if (state.get(DISTANCE) >= 7) {
				randomTick(state, world, pos, random);
				if (shouldUpdateDiagonalLeaves()) {
					TagKey<Block> leavesTag = QuickLeafDecay.getLeavesTag(this);
					getDiagonalPositions(pos).forEach(blockPos -> updateIfMatchingLeaves(world, blockPos, leavesTag, random));
				}
			} else if (world.getBlockState(pos).get(DISTANCE) >= 7) {
				world.scheduleBlockTick(pos, this, getDecayDelay(random));
			}
		}
	}

	@Inject(method = "randomTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
	private void postRemoveBlock(BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random, CallbackInfo ci) {
		if (shouldDoDecayingLeavesEffects()) this.spawnBreakParticles(world, null, pos, state);
	}

	@Unique
	private static Collection<BlockPos> getDiagonalPositions(BlockPos pos) {
		final Collection<BlockPos> diagonalPositions = new LinkedList<>();
		for (Direction direction : Direction.values()) {
			if (direction.getHorizontal() >= 0)
				diagonalPositions.add(pos.offset(direction).offset(direction.rotateYClockwise()));
			else {
				final BlockPos vOffsetPos = pos.offset(direction);
				for (Direction horizontal : Util.HORIZONTAL_DIRECTIONS)
					diagonalPositions.add(vOffsetPos.offset(direction).offset(horizontal.rotateYClockwise()));
			}
		}

		return diagonalPositions;
	}

	@Unique
	private static void updateIfMatchingLeaves(WorldAccess world, BlockPos blockPos, TagKey<Block> leavesTag, RandomGenerator random) {
		final BlockState state = world.getBlockState(blockPos);
		if (state != null && currentLeaves.get().isPresent() && isMatchingLeaves(leavesTag, state, currentLeaves.get().get()))
			world.scheduleBlockTick(blockPos, state.getBlock(), getDecayDelay(random));
	}
}

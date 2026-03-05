package dev.blueon.quickleafdecay;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QuickLeafDecay {
	public static final String NAMESPACE = "quickleafdecay";
	public static final Component NAME = Component.translatable("text." + NAMESPACE + ".name");

	public static final Logger LOGGER = LoggerFactory.getLogger(QuickLeafDecay.class);

	public static final TagKey<Block> LOGS_WITHOUT_LEAVES =
		TagKey.create(Registries.BLOCK, idOf("logs_without_leaves"));
	public static final TagKey<Block> DECAYS_SLOWLY =
		TagKey.create(Registries.BLOCK, idOf("decays_slowly"));

	private static final String LEAVES_GROUPS_SUB_PATH = "leaves_groups/";
	private static final String TREE_TYPES_SUB_PATH = "tree_types/";

	private static final Map<Block, TagKey<Block>> LEAVES_GROUPS = new HashMap<>();
	private static final Map<Block, TagKey<Block>> TREE_TYPES = new HashMap<>();

	public static void updateLeavesGroups(Block leavesBlock) {
		updateBlockTag(leavesBlock, LEAVES_GROUPS, LEAVES_GROUPS_SUB_PATH);
	}

	public static void updateTreeTypes(BlockState state) {
		if (state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
			updateBlockTag(state.getBlock(), TREE_TYPES, TREE_TYPES_SUB_PATH);
		}
	}

	@Nullable
	public static TagKey<Block> getLeavesGroup(Block leavesBlock) {
		return LEAVES_GROUPS.get(leavesBlock);
	}

	@Nullable
	public static TagKey<Block> getTreeType(Block block) {
		return TREE_TYPES.get(block);
	}

	public static void clearTags() {
		LEAVES_GROUPS.clear();
		TREE_TYPES.clear();
	}

	private static void updateBlockTag(Block block, Map<Block, TagKey<Block>> tagMap, String subPath) {
		if (tagMap.get(block) == null) {
			final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			tagMap.put(block, TagKey.create(Registries.BLOCK, id.withPrefix(subPath)));
		}
	}

	public static Identifier idOf(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	public interface PackIds {
		Identifier WOOD_PREVENTS_DECAY = idOf("wood_prevents_decay");
		Identifier YUNGS_BETTER_MINESHAFTS_COMPAT = idOf("yungs_better_mineshafts_compat");
	}
}

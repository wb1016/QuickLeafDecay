package dev.blueon.quickleafdecay;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QuickLeafDecay {
	public static final String NAMESPACE = "quickleafdecay";
	public static final Text NAME = Text.translatable("text." + NAMESPACE + ".name");

	public static final Logger LOGGER = LoggerFactory.getLogger(QuickLeafDecay.class);

	public static final TagKey<Block> LOGS_WITHOUT_LEAVES =
		TagKey.of(RegistryKeys.BLOCK, idOf("logs_without_leaves"));
	public static final TagKey<Block> DECAYS_SLOWLY =
		TagKey.of(RegistryKeys.BLOCK, idOf("decays_slowly"));

	private static final String LEAVES_GROUPS_SUB_PATH = "leaves_groups/";
	private static final String TREE_TYPES_SUB_PATH = "tree_types/";

	private static final Map<Block, TagKey<Block>> LEAVES_GROUPS = new HashMap<>();
	private static final Map<Block, TagKey<Block>> TREE_TYPES = new HashMap<>();

	public static void updateLeavesGroups(Block leavesBlock) {
		updateBlockTag(leavesBlock, LEAVES_GROUPS, LEAVES_GROUPS_SUB_PATH);
	}

	public static void updateTreeTypes(BlockState state) {
		if (state.isIn(BlockTags.LOGS)) {
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
			final Identifier id = Registries.BLOCK.getId(block);
			tagMap.put(block, TagKey.of(RegistryKeys.BLOCK, id.withPrefixedPath(subPath)));
		}
	}

	public static Identifier idOf(String path) {
		return Identifier.of(NAMESPACE, path);
	}

	public interface PackIds {
		Identifier WOOD_PREVENTS_DECAY = idOf("wood_prevents_decay");
		Identifier YUNGS_BETTER_MINESHAFTS_COMPAT = idOf("yungs_better_mineshafts_compat");
	}
}


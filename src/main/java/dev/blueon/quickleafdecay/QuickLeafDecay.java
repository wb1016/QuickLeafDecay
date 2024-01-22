package dev.blueon.quickleafdecay;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class QuickLeafDecay {
	public static final String NAMESPACE = "quickleafdecay";

	private static final String TREE_TYPES_SUB_PATH = "tree_types/";
	private static final String LEAVES_GROUPS_SUB_PATH = "leaves_groups/";
	private static final String LOGS_WITHOUT_LEAVES_PATH = "logs_without_leaves";

	public static final TagKey<Block> LOGS_WITHOUT_LEAVES = TagKey.of(RegistryKeys.BLOCK, new Identifier(NAMESPACE, LOGS_WITHOUT_LEAVES_PATH));

	private static final Map<Block, TagKey<Block>> LEAVES_TAGS = new HashMap<>();
	private static final Map<Block, TagKey<Block>> TREES_TAGS = new HashMap<>();

	public static void updateLeavesTags(Block leavesBlock) {
		updateBlockTag(leavesBlock, LEAVES_TAGS, LEAVES_GROUPS_SUB_PATH);
	}

	public static void updateLogLeavesTags(Block block) {
		updateBlockTag(block, TREES_TAGS, TREE_TYPES_SUB_PATH);
	}

	public static TagKey<Block> getLeavesTag(Block leavesBlock) {
		return LEAVES_TAGS.get(leavesBlock);
	}

	public static TagKey<Block> getLeavesForLog(Block block) {
		return TREES_TAGS.get(block);
	}

	public static void onReload() {
		LEAVES_TAGS.clear();
		TREES_TAGS.clear();
	}

	private static void updateBlockTag(Block block, Map<Block, TagKey<Block>> tagMap, String subPath) {
		if (tagMap.get(block) == null) {
			final Identifier id = Registries.BLOCK.getId(block);
			tagMap.put(block, TagKey.of(RegistryKeys.BLOCK, new Identifier(id.getNamespace(), subPath + id.getPath())));
		}
	}

}

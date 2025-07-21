package dev.blueon.quickleafdecay;

import dev.blueon.quickleafdecay.QuickLeafDecay.PackIds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

import static net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack;

public class Init implements ModInitializer {
    private static void registerCompatPacks(ModContainer thisContainer, Map<String, Identifier> packIdsByModId) {
        packIdsByModId.forEach((modId, packId) -> {
            if (FabricLoader.getInstance().isModLoaded(modId)) {
                registerBuiltinPacks(
                    thisContainer,
                    ResourcePackActivationType.ALWAYS_ENABLED,
                    packId
                );
            }
        });
    }

    private static void registerBuiltinPacks(
        ModContainer thisContainer, ResourcePackActivationType activationType,
        Identifier... ids
    ) {
        for (final var id : ids) {
            registerBuiltinResourcePack(id, thisContainer, packNameOf(id), activationType);
        }
    }

    private static Text packNameOf(Identifier id) {
        return Text.translatable(String.join(".",
            "pack",
            id.getNamespace(),
            id.getPath(),
            "name"
        ));
    }

    @Override
    public void onInitialize() {
        FeatureControl.init();

        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client) {
                QuickLeafDecay.clearTags();
            }
        });

        FabricLoader.getInstance().getModContainer(QuickLeafDecay.NAMESPACE).ifPresent(thisContainer -> {
            registerBuiltinPacks(
                thisContainer, ResourcePackActivationType.NORMAL,
                PackIds.WOOD_PREVENTS_DECAY
            );

            registerCompatPacks(
                thisContainer,
                Map.of(
                    "bettermineshafts", PackIds.YUNGS_BETTER_MINESHAFTS_COMPAT
                )
            );
        });
    }
}

package dev.blueon.quickleafdecay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import static net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class Init implements ModInitializer {
	static {
		FeatureControl.init();
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> QuickLeafDecay.onReload());

		ModContainer thisMod = FabricLoader.getInstance().getModContainer(QuickLeafDecay.NAMESPACE).get();
		registerBuiltinResourcePack(
										new Identifier(QuickLeafDecay.NAMESPACE, "oak_leaves_recognize_jungle_logs"),
										thisMod, ResourcePackActivationType.DEFAULT_ENABLED
		);
		registerBuiltinResourcePack(
										new Identifier(QuickLeafDecay.NAMESPACE, "wood_prevents_decay"),
										thisMod, ResourcePackActivationType.NORMAL
		);
//        FabricLoader.getInstance().getModContainer("wwoo").ifPresent(unused -> registerBuiltinResourcePack(
//            new Identifier(QuickLeafDecay.NAMESPACE, "wwoo_compat"),
//            thisMod, ResourcePackActivationType.ALWAYS_ENABLED
//        ));
	}

	@Override
	public void onInitialize() {
	}
}

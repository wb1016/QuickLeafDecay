package dev.blueon.quickleafdecay;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.text.Text;

import java.util.Optional;

import static dev.blueon.quickleafdecay.QuickLeafDecay.LOGGER;

public final class Util {
    public static Text replace(Text text, String regex, String replacement) {
        String string = text.getString();
        string = string.replaceAll(regex, replacement);
        return Text.literal(string).setStyle(text.getStyle());
    }

    public static boolean isModLoaded(String id, String versionPredicate) {
        final Optional<ModContainer> optModContainer = FabricLoader.getInstance().getModContainer(id);
        if (optModContainer.isPresent()){
            try {
                return VersionPredicate.parse(versionPredicate).test(optModContainer.get().getMetadata().getVersion());
            } catch (VersionParsingException e) {
                LOGGER.error("Failed to parse version", e);
            }
        }

        return false;
    }

    private Util() { }
}

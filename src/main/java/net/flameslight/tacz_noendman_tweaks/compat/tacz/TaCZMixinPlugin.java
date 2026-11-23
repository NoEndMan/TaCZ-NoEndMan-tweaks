package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin config plugin of the TaCZ tweaks: applies their mixins only when TaCZ is installed.
 */
public final class TaCZMixinPlugin implements IMixinConfigPlugin {

    /*
        TaCZ's mod id. A literal, so TaCZCompat (which reads ModList, not ready yet at mixin time) is not loaded here.
     */
    private static final String TACZ_MOD_ID = "tacz";

    /*
        Whether TaCZ is in the mod list, read once when the mixin config loads.
     */
    private boolean taczPresent;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        this.taczPresent = mods != null && mods.getModFileById(TACZ_MOD_ID) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Applies a TaCZ tweak mixin only when TaCZ is installed.
     *
     * @param targetClassName the target class
     * @param mixinClassName  the mixin class
     * @return true when TaCZ is installed
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.taczPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

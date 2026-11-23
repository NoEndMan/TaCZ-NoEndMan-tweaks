package net.flameslight.tacz_noendman_tweaks.compat.oculus;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin config plugin of the Oculus Distant Horizons depth fix: applies its mixins only when both Oculus and Distant
 * Horizons are installed, as the fix does nothing without either of them.
 */
public final class OculusDhMixinPlugin implements IMixinConfigPlugin {

    /* Mod ids, literals so no class that reads ModList (not ready yet at mixin time) is loaded here. */
    private static final String OCULUS_MOD_ID = "oculus";
    private static final String DH_MOD_ID = "distanthorizons";

    /*
        Whether both mods are in the mod list, read once when the mixin config loads.
     */
    private boolean bothPresent;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        this.bothPresent = mods != null && mods.getModFileById(OCULUS_MOD_ID) != null
                && mods.getModFileById(DH_MOD_ID) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Applies a fix mixin only when Oculus and Distant Horizons are both installed.
     *
     * @param targetClassName the target class
     * @param mixinClassName  the mixin class
     * @return true when both are installed
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.bothPresent;
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

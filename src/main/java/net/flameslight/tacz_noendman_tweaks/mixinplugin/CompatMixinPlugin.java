package net.flameslight.tacz_noendman_tweaks.mixinplugin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin of the optional-mod config. Applies its mixins only when Immersive Aircraft is installed,
 * because the upper body twist only exists while flying an Immersive Aircraft vehicle.
 */
public class CompatMixinPlugin implements IMixinConfigPlugin {

    private static final String IMMERSIVE_AIRCRAFT_ID = "immersive_aircraft";

    private boolean aircraftLoaded;

    /**
     * Reads the loading mod list once.
     *
     * @param mixinPackage the mixin package of this config
     */
    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        this.aircraftLoaded = mods != null && mods.getModFileById(IMMERSIVE_AIRCRAFT_ID) != null;
    }

    /**
     * No custom refmap.
     *
     * @return null, to use the refmap named in the config
     */
    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Applies the compat mixins only when Immersive Aircraft is installed.
     *
     * @param targetClassName the class the mixin targets
     * @param mixinClassName  the mixin class
     * @return true when Immersive Aircraft is installed
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.aircraftLoaded;
    }

    /**
     * Not used.
     *
     * @param myTargets    the targets of this config
     * @param otherTargets the targets of other configs
     */
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    /**
     * No extra mixins.
     *
     * @return null
     */
    @Override
    public List<String> getMixins() {
        return null;
    }

    /**
     * Not used.
     *
     * @param targetClassName the target class name
     * @param targetClass     the target class node
     * @param mixinClassName  the mixin class name
     * @param mixinInfo       the mixin info
     */
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    /**
     * Not used.
     *
     * @param targetClassName the target class name
     * @param targetClass     the target class node
     * @param mixinClassName  the mixin class name
     * @param mixinInfo       the mixin info
     */
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

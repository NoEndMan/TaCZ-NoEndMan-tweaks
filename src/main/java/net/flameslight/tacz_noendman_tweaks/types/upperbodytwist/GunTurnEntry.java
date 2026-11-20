package net.flameslight.tacz_noendman_tweaks.types.upperbodytwist;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * One player render tracked by EmfGunTwist.
 */
public final class GunTurnEntry {

    private final LivingEntity entity;
    private final ModelPart body;
    private final TwistPass pass;

    /**
     * Creates the record of one render.
     *
     * @param entity the player
     * @param body   the model's body part
     * @param pass   the gun turn, or null when there is none
     */
    public GunTurnEntry(LivingEntity entity, ModelPart body, TwistPass pass) {
        this.entity = entity;
        this.body = body;
        this.pass = pass;
    }

    /**
     * Returns the player of this render.
     *
     * @return the player
     */
    public LivingEntity getEntity() {
        return this.entity;
    }

    /**
     * Tells whether this render is of the model with the given body part.
     *
     * @param body the body part, may be null
     * @return true when it is this render's body
     */
    public boolean isFor(ModelPart body) {
        return body != null && body == this.body;
    }

    /**
     * Applies the gun turn again to what EMF posed, after its animation or after it overwrites parts.
     *
     * @return true when this render has a gun turn
     */
    public boolean reapply() {
        if (this.pass == null)
            return false;

        this.pass.reapply();
        return true;
    }

    /**
     * Puts the untwisted poses back before EMF's animation.
     */
    public void untwist() {
        if (this.pass != null)
            this.pass.untwist();
    }

    /**
     * Restores the original poses.
     */
    public void restore() {
        if (this.pass != null)
            this.pass.restore();
    }
}

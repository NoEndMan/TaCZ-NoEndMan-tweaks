package net.flameslight.tacz_noendman_tweaks.compat.tacz;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.AttachmentRender;
import com.tacz.guns.client.renderer.item.ExternalGunItemVboRenderer;
import com.tacz.guns.client.renderer.item.StaticGunRenderResult;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.config.client.ResourceConfig;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.util.RenderHelper;
import net.flameslight.tacz_noendman_tweaks.config.ClientConfig;
import net.flameslight.tacz_noendman_tweaks.logger.ModLogger;
import net.flameslight.tacz_noendman_tweaks.mixin.tacz.client.ClientAttachmentIndexAccessor;
import net.flameslight.tacz_noendman_tweaks.mixin.tacz.client.GunDisplayInstanceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pre-renders, once per game session on the render thread, the shared first equip render code, hidden: the first
 * person arms, TACZ's full first person gun model render of one sample gun, one attachment of each type, and, only
 * while a shader pack is active, the sample gun's third person static mesh (drawn by the shadow pass). The real first
 * equip of any gun then finds that code, its classes and the mods' caches ready. Client only, only call when
 * TaczCompat.LOADED is true.
 */
public final class TaczFirstEquipPreRender {

    /** Client ticks between two checks of a waited for model or of the shader pack state. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Client ticks the arm pre-render waits at most for the player's skin (10 seconds). */
    private static final int SKIN_WAIT_LIMIT_TICKS = 200;

    /** Tries of the third person pre-render while TACZ still builds the static mesh (one per client tick). */
    private static final int THIRD_PERSON_MAX_TRIES = 40;

    /*
        Client thread only: done flags of this game session, ticks since the world join, and whether the tick listener
        is registered.
     */
    private static boolean armsDone = false;
    private static boolean modelDone = false;
    private static boolean attachmentsDone = false;
    private static boolean thirdPersonDone = false;
    private static int ticks = 0;
    private static boolean registered = false;

    /*
        Client thread only: whether the gun model pre-render made its first check since the world join (the first check
        runs right after the arms, later ones every CHECK_INTERVAL_TICKS).
     */
    private static boolean modelChecked = false;

    /*
        Client thread only: the gun model pre-render's sample gun (its stack and display), picked once, and TACZ's
        resource revision when it was picked (a resource reload replaces TACZ's guns, so it is picked again after one).
     */
    private static ItemStack sampleStack = null;
    private static GunDisplayInstance sampleDisplay = null;
    private static long sampleRevision = 0L;

    /*
        Client thread only: the sample gun stack kept after the gun model pre-render, for the attachment and third person
        pre-renders.
     */
    private static ItemStack warmGunStack = null;

    /*
        Client thread only: the attachment pre-render's picks (one attachment per type, null before picking), and its
        total time and count for the log.
     */
    private static EnumMap<AttachmentType, Map.Entry<ResourceLocation, ClientAttachmentIndex>> attachmentPicks = null;
    private static long attachmentNs = 0L;
    private static int attachmentCount = 0;

    /*
        Client thread only: the third person pre-render's offscreen target (the static mesh draws with its own GL draw
        calls, so it is drawn there instead of on screen), its tries and total time.
     */
    private static TextureTarget hiddenTarget = null;
    private static int thirdPersonTries = 0;
    private static long thirdPersonNs = 0L;

    /* The client tick listener, registered only while a pre-render is still due. */
    private static final PreRenderTick TICK = new PreRenderTick();

    private TaczFirstEquipPreRender() {
    }

    /**
     * Registers the world join and leave listeners. Called once from client setup.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TaczFirstEquipPreRender::onLoggingIn);
        MinecraftForge.EVENT_BUS.addListener(TaczFirstEquipPreRender::onLoggingOut);
    }

    /**
     * World join: starts the tick listener when a pre-render is still due this session.
     *
     * @param event the login event
     */
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!armsDue() && !modelDue() && !attachmentsDue() && !thirdPersonDue())
            return;

        ticks = 0;
        modelChecked = false;
        setRegistered(true);
    }

    /**
     * World leave: stops the tick listener. Pre-renders still due run at the next world join.
     *
     * @param event the logout event
     */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        setRegistered(false);
        clearState();
    }

    /**
     * Tells whether the arm pre-render is enabled and still not done this session.
     *
     * @return true when it is due
     */
    private static boolean armsDue() {
        return !armsDone && ClientConfig.isFirstEquipWarmUp();
    }

    /**
     * Tells whether the gun model pre-render is enabled and still not done this session.
     *
     * @return true when it is due
     */
    private static boolean modelDue() {
        return !modelDone && ClientConfig.isFirstEquipWarmUp();
    }

    /**
     * Tells whether the attachment pre-render is enabled and still not done this session (it waits for the gun model).
     *
     * @return true when it is due
     */
    private static boolean attachmentsDue() {
        return !attachmentsDone && ClientConfig.isFirstEquipWarmUp();
    }

    /**
     * Tells whether the third person pre-render is enabled and still not done this session (it waits for the gun model
     * and for a shader pack).
     *
     * @return true when it is due
     */
    private static boolean thirdPersonDue() {
        return !thirdPersonDone && ClientConfig.isFirstEquipWarmUp();
    }

    /**
     * Registers or unregisters the tick listener, once each.
     *
     * @param value true to register
     */
    private static void setRegistered(boolean value) {
        if (value == registered)
            return;

        if (value)
            MinecraftForge.EVENT_BUS.register(TICK);
        else
            MinecraftForge.EVENT_BUS.unregister(TICK);

        registered = value;
    }

    /**
     * Clears the session's waiting state (samples, picks, offscreen target), not its done flags.
     */
    private static void clearState() {
        clearSample();
        warmGunStack = null;
        attachmentPicks = null;
        thirdPersonTries = 0;
        releaseHiddenTarget();
    }

    /**
     * Renders both first person arms through TACZ's own arm render, like its gun arm renderers do, into buffers that
     * are never drawn.
     *
     * @param player     the local player
     * @param skinLoaded whether the player's skin finished loading, for the log
     */
    private static void preRenderArms(LocalPlayer player, boolean skinLoaded) {
        long start = System.nanoTime();
        DiscardingBufferSource source = new DiscardingBufferSource();

        try {
            RenderHelper.renderFirstPersonArm(player, HumanoidArm.RIGHT, new PoseStack(), LightTexture.FULL_BRIGHT, source);
            RenderHelper.renderFirstPersonArm(player, HumanoidArm.LEFT, new PoseStack(), LightTexture.FULL_BRIGHT, source);
            ModLogger.debug("pre-render: arms {} ms (skin loaded {})", elapsedMs(start), skinLoaded);
        } catch (Throwable error) {
            ModLogger.warn("First equip pre-render: arms failed, skipped for this session: {}", error.toString());
        } finally {
            source.endBatch();
        }
    }

    /**
     * Gun model pre-render step: picks the sample gun once (again only after a resource reload), then checks only its
     * loaded flag, and pre-renders its model once it is loaded.
     *
     * @param player the local player
     */
    private static void updateModel(LocalPlayer player) {
        long revision = ClientIndexManager.resourceRevision();

        if (sampleDisplay != null && revision != sampleRevision)
            clearSample();

        if (sampleDisplay == null) {
            pickSampleGun(player);
            sampleRevision = revision;

            // TACZ has no gun at all (its gun list is loaded before joining), so there is nothing to pre-render.
            if (sampleDisplay == null) {
                ModLogger.debug("pre-render: no TACZ gun found, gun pre-renders skipped");
                modelDone = true;
                attachmentsDone = true;
                thirdPersonDone = true;
                return;
            }
        }

        GunDisplayInstanceAccessor accessor = (GunDisplayInstanceAccessor) sampleDisplay;

        if (accessor.tacz_noendman$isModelLoaded()) {
            preRenderGunModel(sampleStack, sampleDisplay);
            warmGunStack = sampleStack;
            modelDone = true;
            clearSample();
        } else if (accessor.tacz_noendman$isModelLoadFailed()) {
            ModLogger.debug("pre-render: sample gun model {} failed to load, gun model skipped", sampleDisplay.getDisplayId());
            warmGunStack = sampleStack;
            modelDone = true;
            clearSample();
        }
    }

    /**
     * Picks the sample gun, once: a carried gun with a loaded model, else any registered gun with a loaded model, else
     * a carried gun (or any registered gun) whose model TACZ is then asked to load in the background. Never waits for a
     * load (TACZ's getGunModel would block until the model is loaded). Sets sampleStack and sampleDisplay, or leaves
     * them null when TACZ has no gun at all.
     *
     * @param player the local player
     */
    private static void pickSampleGun(LocalPlayer player) {
        ItemStack fallbackStack = null;
        GunDisplayInstance fallbackDisplay = null;

        for (ItemStack stack : player.getInventory().items) {
            if (IGun.getIGunOrNull(stack) == null)
                continue;

            Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(stack);

            if (display.isEmpty())
                continue;

            if (isLoaded(display.get())) {
                setSample(stack.copy(), display.get());
                return;
            }

            if (fallbackDisplay == null) {
                fallbackStack = stack.copy();
                fallbackDisplay = display.get();
            }
        }

        for (Map.Entry<ResourceLocation, ClientGunIndex> entry : TimelessAPI.getAllClientGunIndex()) {
            ItemStack stack = GunItemBuilder.create().setId(entry.getKey()).build();
            Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(stack);

            if (display.isEmpty())
                continue;

            if (isLoaded(display.get())) {
                setSample(stack, display.get());
                return;
            }

            if (fallbackDisplay == null) {
                fallbackStack = stack;
                fallbackDisplay = display.get();
            }
        }

        if (fallbackDisplay == null)
            return;

        setSample(fallbackStack, fallbackDisplay);

        /* Starts TACZ's own background load. Only with TACZ's lazy loading on: without it TACZ loads every model at
           resource load, and its warmUpModel would load on this thread instead. */
        if (ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get())
            fallbackDisplay.warmUpModel();
    }

    /**
     * Sets the sample gun.
     *
     * @param stack   the gun stack
     * @param display the gun's display
     */
    private static void setSample(ItemStack stack, GunDisplayInstance display) {
        sampleStack = stack;
        sampleDisplay = display;
    }

    /**
     * Clears the sample gun.
     */
    private static void clearSample() {
        sampleStack = null;
        sampleDisplay = null;
    }

    /**
     * Reads whether TACZ finished loading a gun's model, without waiting.
     *
     * @param display the gun's display
     * @return true when its model is loaded
     */
    private static boolean isLoaded(GunDisplayInstance display) {
        return ((GunDisplayInstanceAccessor) display).tacz_noendman$isModelLoaded();
    }

    /**
     * Runs TACZ's full first person gun model render for the sample gun, with the render type and display context
     * TACZ's first person render uses, into buffers that are never drawn. Its functional parts (arms, attachments,
     * muzzle flash, shells, text) run too, and their queued drawing runs and is cleared inside the same render call.
     *
     * @param stack   the sample gun stack
     * @param display the sample gun's display, with its model loaded
     */
    private static void preRenderGunModel(ItemStack stack, GunDisplayInstance display) {
        long start = System.nanoTime();
        DiscardingBufferSource source = new DiscardingBufferSource();

        try {
            BedrockGunModel model = display.getGunModel();
            ResourceLocation texture = display.getModelTexture();

            if (model == null || texture == null)
                return;

            RenderType type = display.enablesTransparency() ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
            model.render(new PoseStack(), stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, type, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, source);
            ModLogger.debug("pre-render: gun model {} ms (gun {})", elapsedMs(start), display.getDisplayId());
        } catch (Throwable error) {
            ModLogger.warn("First equip pre-render: gun model failed, skipped for this session: {}", error.toString());
        } finally {
            source.endBatch();
        }
    }

    /**
     * Attachment pre-render step: picks one attachment per type once (a loaded one when possible, else TACZ is asked to
     * load one in the background), then renders each pick once its models are loaded, until every pick is done.
     */
    private static void updateAttachments() {
        if (attachmentPicks == null)
            pickAttachments();

        Iterator<Map.Entry<AttachmentType, Map.Entry<ResourceLocation, ClientAttachmentIndex>>> picks =
                attachmentPicks.entrySet().iterator();

        while (picks.hasNext()) {
            Map.Entry<AttachmentType, Map.Entry<ResourceLocation, ClientAttachmentIndex>> pick = picks.next();
            ClientAttachmentIndexAccessor accessor = (ClientAttachmentIndexAccessor) pick.getValue().getValue();

            if (accessor.tacz_noendman$isModelsLoaded()) {
                preRenderAttachment(pick.getKey(), pick.getValue().getKey());
                picks.remove();
            } else if (accessor.tacz_noendman$isModelsLoadFailed()) {
                picks.remove();
            }
        }

        if (attachmentPicks.isEmpty()) {
            ModLogger.debug("pre-render: attachments {} ms ({} types)", attachmentNs / 1_000_000L, attachmentCount);
            attachmentsDone = true;
            attachmentPicks = null;
        }
    }

    /**
     * Picks one registered attachment per attachment type, preferring one whose models are already loaded, and asks
     * TACZ to load the others in the background. Never waits for a load.
     */
    private static void pickAttachments() {
        attachmentPicks = new EnumMap<>(AttachmentType.class);

        for (Map.Entry<ResourceLocation, ClientAttachmentIndex> entry : TimelessAPI.getAllClientAttachmentIndex()) {
            AttachmentType type = TimelessAPI.getCommonAttachmentIndex(entry.getKey()).map(CommonAttachmentIndex::getType).orElse(null);

            if (type == null || type == AttachmentType.NONE)
                continue;

            Map.Entry<ResourceLocation, ClientAttachmentIndex> current = attachmentPicks.get(type);

            if (current == null || (!isLoaded(current.getValue()) && isLoaded(entry.getValue())))
                attachmentPicks.put(type, Map.entry(entry.getKey(), entry.getValue()));
        }

        boolean lazy = ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD.get();

        for (Map.Entry<ResourceLocation, ClientAttachmentIndex> pick : attachmentPicks.values()) {
            if (lazy && !isLoaded(pick.getValue()))
                pick.getValue().warmUp();
        }
    }

    /**
     * Reads whether TACZ finished loading an attachment's models, without waiting.
     *
     * @param index the attachment's client index
     * @return true when its models are loaded
     */
    private static boolean isLoaded(ClientAttachmentIndex index) {
        return ((ClientAttachmentIndexAccessor) index).tacz_noendman$isModelsLoaded();
    }

    /**
     * Renders one attachment through TACZ's own attachment render, with the sample gun and the first person display
     * context, into buffers that are never drawn.
     *
     * @param type the attachment type
     * @param id   the attachment id
     */
    private static void preRenderAttachment(AttachmentType type, ResourceLocation id) {
        long start = System.nanoTime();
        DiscardingBufferSource source = new DiscardingBufferSource();

        try {
            ItemStack attachment = AttachmentItemBuilder.create().setId(id).build();
            AttachmentRender.renderAttachment(attachment, warmGunStack, type, new PoseStack(),
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, source);
            attachmentCount++;
        } catch (Throwable error) {
            ModLogger.warn("First equip pre-render: attachment {} failed, skipped: {}", id, error.toString());
        } finally {
            source.endBatch();
            attachmentNs += System.nanoTime() - start;
        }
    }

    /**
     * Third person pre-render step, only while a shader pack is active (the shadow pass is what draws the local
     * player's third person gun): draws the sample gun through TACZ's static mesh path into a small offscreen target,
     * once per tick, until TACZ reports the mesh ready (or unsupported) or THIRD_PERSON_MAX_TRIES ran out.
     */
    private static void updateThirdPerson() {
        long start = System.nanoTime();
        StaticGunRenderResult result;

        try {
            result = renderThirdPersonHidden(warmGunStack);
        } catch (Throwable error) {
            ModLogger.warn("First equip pre-render: third person failed, skipped for this session: {}", error.toString());
            finishThirdPerson();
            return;
        }

        thirdPersonNs += System.nanoTime() - start;
        thirdPersonTries++;

        if (result.submitted() || result == StaticGunRenderResult.UNSUPPORTED || thirdPersonTries >= THIRD_PERSON_MAX_TRIES) {
            ModLogger.debug("pre-render: third person {} ms (result {})", thirdPersonNs / 1_000_000L, result.status());
            finishThirdPerson();
        }
    }

    /**
     * Marks the third person pre-render done and frees its offscreen target.
     */
    private static void finishThirdPerson() {
        thirdPersonDone = true;
        releaseHiddenTarget();
    }

    /**
     * Draws a gun through TACZ's third person static mesh path with the main render target unbound: the draw lands in a
     * small offscreen target that is never shown, and the main target is bound again right after.
     *
     * @param stack the gun stack
     * @return TACZ's static mesh render result
     */
    private static StaticGunRenderResult renderThirdPersonHidden(ItemStack stack) {
        if (hiddenTarget == null)
            hiddenTarget = new TextureTarget(16, 16, true, Minecraft.ON_OSX);

        DiscardingBufferSource source = new DiscardingBufferSource();
        hiddenTarget.bindWrite(true);

        try {
            return ExternalGunItemVboRenderer.tryRender(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, new PoseStack(),
                    source, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } finally {
            source.endBatch();
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    /**
     * Frees the third person pre-render's offscreen target.
     */
    private static void releaseHiddenTarget() {
        if (hiddenTarget == null)
            return;

        hiddenTarget.destroyBuffers();
        hiddenTarget = null;
    }

    /**
     * Milliseconds since a start time.
     *
     * @param startNs the start, from System.nanoTime
     * @return the elapsed milliseconds
     */
    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    /**
     * A vanilla buffer source whose batch ends discard the vertices instead of drawing them, so a pre-render never
     * reaches the screen and makes no draw call. Other mods' hooks on getBuffer still run, as in a real render.
     */
    private static final class DiscardingBufferSource extends MultiBufferSource.BufferSource {

        private DiscardingBufferSource() {
            super(new BufferBuilder(256), Map.of());
        }

        /**
         * Ends a render type's batch like vanilla, but releases its vertices instead of drawing them.
         *
         * @param type the render type
         */
        @Override
        public void endBatch(RenderType type) {
            BufferBuilder builder = this.fixedBuffers.getOrDefault(type, this.builder);
            boolean last = Objects.equals(this.lastState, type.asOptional());

            if ((last || builder != this.builder) && this.startedBuffers.remove(builder)) {
                BufferBuilder.RenderedBuffer batch = builder.endOrDiscardIfEmpty();

                if (batch != null)
                    batch.release();

                if (last)
                    this.lastState = Optional.empty();
            }
        }
    }

    /**
     * Client tick listener, registered only while a pre-render is still due. In order: the arms once the player's skin
     * is loaded (or after SKIN_WAIT_LIMIT_TICKS), the gun model once the sample gun's model is loaded, then one
     * attachment of each type once their models are loaded, then the third person static mesh only while a shader pack
     * is active. Waits are checked every CHECK_INTERVAL_TICKS. Unregisters itself when all are done.
     */
    private static final class PreRenderTick {

        /**
         * Runs the pre-renders that are due, at the end of the client tick.
         *
         * @param event the client tick event
         */
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END)
                return;

            LocalPlayer player = Minecraft.getInstance().player;

            if (player == null)
                return;

            ticks++;
            boolean interval = ticks % CHECK_INTERVAL_TICKS == 0;

            if (armsDue()) {
                boolean skinLoaded = player.isSkinLoaded();

                if (!skinLoaded && ticks < SKIN_WAIT_LIMIT_TICKS)
                    return;

                preRenderArms(player, skinLoaded);
                armsDone = true;
            }

            // The gun model render draws the arms too, so it runs only after the arm pre-render used the loaded skin.
            if (modelDue() && (!modelChecked || interval)) {
                modelChecked = true;
                updateModel(player);
            }

            // The attachment and third person pre-renders use the sample gun, so they wait for the gun model.
            if (modelDone && warmGunStack != null) {
                if (attachmentsDue() && (attachmentPicks == null || interval))
                    updateAttachments();

                if (thirdPersonDue() && (thirdPersonTries > 0 || (interval && OculusCompat.isUsingRenderPack())))
                    updateThirdPerson();
            }

            // Also stops when the option was switched off. A pre-render switched on later runs at the next world join.
            if (!armsDue() && !modelDue() && !attachmentsDue() && !thirdPersonDue()) {
                setRegistered(false);
                clearState();
            }
        }
    }
}

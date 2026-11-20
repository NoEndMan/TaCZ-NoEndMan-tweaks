package net.flameslight.tacz_noendman_tweaks.compat.oculus;

/**
 * Keeps Distant Horizons' depth attached to Oculus's DH framebuffers. When Iris re-attaches Minecraft's main depth to
 * its framebuffers (after the main depth buffer is recreated, for example by a stencil enable), the next DH depth
 * reconnect is forced to attach DH's depth again.
 */
public final class OculusDhDepthGuard {

    /*
        Set when Iris re-attached the main depth, cleared by the next DH depth reconnect.
     */
    private static volatile boolean reattachPending = false;

    /**
     * Records that Iris re-attached the main depth to its owned framebuffers.
     */
    public static void markReattached() {
        reattachPending = true;
    }

    /**
     * Tells whether the DH depth must be re-attached, and clears the request.
     *
     * @return true once after each main depth re-attach
     */
    public static boolean consume() {
        boolean pending = reattachPending;
        reattachPending = false;

        return pending;
    }
}

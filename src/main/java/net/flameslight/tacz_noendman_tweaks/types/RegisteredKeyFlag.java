package net.flameslight.tacz_noendman_tweaks.types;

/**
 * Added to TACZ's SyncedDataKey by a mixin. Marks a key object that TACZ's registry accepted,
 * so the registration check in get and set can skip its hash set lookup.
 */
public interface RegisteredKeyFlag {

    /**
     * Tells whether this exact key object was registered.
     *
     * @return true after TACZ's registerDataKey accepted this key
     */
    boolean tacz_noendman$isRegistered();

    /**
     * Marks this key object as registered. Called once, right after TACZ registered it.
     */
    void tacz_noendman$markRegistered();
}

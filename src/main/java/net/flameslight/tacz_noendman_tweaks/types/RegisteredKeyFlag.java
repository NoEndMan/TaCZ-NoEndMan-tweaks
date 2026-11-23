package net.flameslight.tacz_noendman_tweaks.types;

/**
 * Added to TaCZ's SyncedDataKey by a mixin. Marks a key object that TaCZ's registry accepted,
 * so the registration check in get and set can skip its hash set lookup.
 */
public interface RegisteredKeyFlag {

    /**
     * Tells whether this exact key object was registered.
     *
     * @return true after TaCZ's registerDataKey accepted this key
     */
    boolean tacz_noendman$isRegistered();

    /**
     * Marks this key object as registered. Called once, right after TaCZ registered it.
     */
    void tacz_noendman$markRegistered();
}

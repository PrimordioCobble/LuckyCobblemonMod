package net.crulim.luckblockcobblemon.platform;

/**
 * Holds the active {@link LoaderPlatform}. Each loader sets it before calling the common init.
 */
public final class Platform {
    private static LoaderPlatform current;

    private Platform() {
    }

    public static void set(LoaderPlatform platform) {
        current = platform;
    }

    public static LoaderPlatform get() {
        if (current == null) {
            throw new IllegalStateException("Lucky Block Cobblemon platform was not initialized by the mod loader.");
        }
        return current;
    }
}

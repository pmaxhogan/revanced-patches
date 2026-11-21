package app.morphe.extension.youtube.patches.shorts;

import static app.morphe.extension.shared.utils.Utils.showToastShort;

import app.morphe.extension.shared.utils.Logger;

/**
 * Vantage-only. Kept in its own file so the fork's delta in {@link ShortsPatch} is a
 * single line and the nightly upstream sync rarely conflicts.
 */
final class VantageShorts {
    private VantageShorts() {
    }

    /**
     * Vantage hard-disables Shorts. {@link ShortsPatch#openShortInRegularPlayer(String)} is
     * injected unconditionally at the Shorts playback-start seam (PlaybackStartDescriptor
     * dispatch), so it is reached for EVERY entry point: the bottom-nav Shorts tab, the
     * launcher shortcut, the home-screen widget, a direct Shorts link, a Short inside a
     * playlist or the Liked Videos playlist, and the channel Shorts tab.
     * Returning true there tells the injected code the launch was handled, so the native
     * Shorts player is never shown - and we deliberately open nothing in its place.
     * No Short can play by any means; regular videos are untouched.
     *
     * @return always true: the Short launch is blocked.
     */
    static boolean blockShort() {
        try {
            showToastShort("Shorts are disabled");
        } catch (Exception ex) {
            Logger.printException(() -> "VantageShorts blockShort failure", ex);
        }
        return true;
    }
}

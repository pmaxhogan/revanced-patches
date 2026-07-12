package app.morphe.extension.youtube.patches.shorts;

import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.shared.utils.Utils.showToastShort;
import static app.morphe.extension.shared.utils.Utils.validateValue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.ShortsPlayerState;
import kotlin.Unit;

@SuppressWarnings("unused")
public class ShortsPatch {
    public enum ShortsPlayerType {
        SHORTS_PLAYER,
        REGULAR_PLAYER,
        REGULAR_PLAYER_FULLSCREEN
    }

    private static final boolean ENABLE_SHORTS_TIME_STAMP =
            Settings.ENABLE_SHORTS_TIME_STAMP.get();
    private static final boolean ENABLE_SHORTS_CLEAR_MODE =
            ENABLE_SHORTS_TIME_STAMP && Settings.ENABLE_SHORTS_CLEAR_MODE.get();
    public static final boolean HIDE_SHORTS_NAVIGATION_BAR =
            Settings.HIDE_SHORTS_NAVIGATION_BAR.get();
    private static final double NAVIGATION_BAR_HEIGHT_PERCENTAGE;
    private static int navigationBarHeight = -1;

    static {
        if (HIDE_SHORTS_NAVIGATION_BAR) {
            ShortsPlayerState.getOnChange().addObserver((ShortsPlayerState state) -> {
                setNavigationBarLayoutParams(state);
                return Unit.INSTANCE;
            });
        }

        final int heightPercentage = validateValue(
                Settings.SHORTS_NAVIGATION_BAR_HEIGHT_PERCENTAGE,
                0,
                100,
                "revanced_shorts_navigation_bar_height_percentage_invalid_toast"
        );

        NAVIGATION_BAR_HEIGHT_PERCENTAGE = heightPercentage / 100d;
    }

    public static boolean disableResumingStartupShortsPlayer() {
        return Settings.DISABLE_RESUMING_SHORTS_PLAYER.get();
    }

    public static boolean disableResumingStartupShortsPlayer(boolean original) {
        return !Settings.DISABLE_RESUMING_SHORTS_PLAYER.get() && original;
    }

    public static boolean enableShortsTimeStamp(boolean original) {
        return ENABLE_SHORTS_TIME_STAMP || original;
    }

    // If this is not overridden, timestamps will not be enabled on Shorts played on the channel.
    public static int enableShortsTimeStamp(int original) {
        return ENABLE_SHORTS_TIME_STAMP ? 10010 : original;
    }

    public static boolean enableShortsTimeStampReverse(boolean original) {
        return !ENABLE_SHORTS_TIME_STAMP && original;
    }

    public static boolean enableShortsClearMode(boolean original) {
        return ENABLE_SHORTS_CLEAR_MODE || original;
    }

    public static void hideShortsCommentsButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_COMMENTS_BUTTON.get(), view);
    }

    public static ViewGroup hideShortsInfoPanel(ViewGroup viewGroup) {
        return Settings.HIDE_SHORTS_INFO_PANEL.get() ? null : viewGroup;
    }

    public static boolean hideShortsLikeButton() {
        return Settings.HIDE_SHORTS_LIKE_BUTTON.get();
    }

    public static void hideShortsRemixButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_REMIX_BUTTON.get(), view);
    }

    public static void hideShortsShareButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_SHARE_BUTTON.get(), view);
    }

    public static boolean hideShortsSoundButton() {
        return Settings.HIDE_SHORTS_SOUND_BUTTON.get();
    }

    private static final int zeroPaddingDimenId =
            ResourceUtils.getDimenIdentifier("revanced_zero_padding");

    public static int getShortsSoundButtonDimenId(int dimenId) {
        return Settings.HIDE_SHORTS_SOUND_BUTTON.get()
                ? zeroPaddingDimenId
                : dimenId;
    }

    public static int hideShortsSubscribeButton(int original) {
        return Settings.HIDE_SHORTS_SUBSCRIBE_BUTTON.get() ? 0 : original;
    }

    // YouTube 18.29.38 ~ YouTube 19.28.42
    public static boolean hideShortsPausedHeader() {
        return Settings.HIDE_SHORTS_PAUSED_HEADER.get();
    }

    // YouTube 19.29.42 ~
    public static boolean hideShortsPausedHeader(boolean original) {
        return Settings.HIDE_SHORTS_PAUSED_HEADER.get() || original;
    }

    public static boolean hideShortsToolBar(boolean original) {
        return !Settings.HIDE_SHORTS_TOOLBAR.get() && original;
    }

    /**
     * BottomBarContainer is the parent view of {@link PivotBar},
     * And can be hidden using {@link View#setVisibility} only when it is initialized.
     * <p>
     * If it was not hidden with {@link View#setVisibility} when it was initialized,
     * it should be hidden with {@link FrameLayout.LayoutParams}.
     * <p>
     * When Shorts is opened, {@link FrameLayout.LayoutParams} should be changed to 0dp,
     * When Shorts is closed, {@link FrameLayout.LayoutParams} should be changed to the original.
     */
    private static WeakReference<View> bottomBarContainerRef = new WeakReference<>(null);

    private static FrameLayout.LayoutParams originalLayoutParams;
    private static final FrameLayout.LayoutParams zeroLayoutParams =
            new FrameLayout.LayoutParams(0, 0);

    public static void setNavigationBar(View view) {
        if (!HIDE_SHORTS_NAVIGATION_BAR) {
            return;
        }
        bottomBarContainerRef = new WeakReference<>(view);
        if (!(view.getLayoutParams() instanceof FrameLayout.LayoutParams lp)) {
            return;
        }
        if (originalLayoutParams == null) {
            originalLayoutParams = lp;
        }
    }

    public static int setNavigationBarHeight(int original) {
        if (HIDE_SHORTS_NAVIGATION_BAR) {
            if (navigationBarHeight == -1) {
                navigationBarHeight = (int) Math.round(original * NAVIGATION_BAR_HEIGHT_PERCENTAGE);
            }
            return navigationBarHeight;
        }
        return original;
    }

    private static void setNavigationBarLayoutParams(@NonNull ShortsPlayerState shortsPlayerState) {
        final View navigationBar = bottomBarContainerRef.get();
        if (navigationBar == null) {
            return;
        }
        if (!(navigationBar.getLayoutParams() instanceof FrameLayout.LayoutParams lp)) {
            return;
        }
        navigationBar.setLayoutParams(
                shortsPlayerState.isClosed()
                        ? originalLayoutParams
                        : zeroLayoutParams
        );
    }

    public static boolean restoreShortsOldPlayerLayout() {
        return !Settings.RESTORE_SHORTS_OLD_PLAYER_LAYOUT.get();
    }

    public static boolean openShortInRegularPlayer(String videoId) {
        // Vantage hard-disables Shorts. This hook is injected unconditionally at the
        // Shorts playback-start seam (PlaybackStartDescriptor dispatch), so it is
        // reached for EVERY entry point: the bottom-nav Shorts tab, the launcher
        // shortcut, the home-screen widget, a direct Shorts link, a Short inside a
        // playlist or the Liked Videos playlist, and the channel Shorts tab.
        // Returning true tells the injected code the launch was handled, so the
        // native Shorts player is never shown - and we deliberately open nothing in
        // its place. No Short can play by any means; regular videos are untouched.
        try {
            showToastShort("Shorts are disabled");
        } catch (Exception ex) {
            Logger.printException(() -> "openShortInRegularPlayer (blocked) failure", ex);
        }
        return true;
    }

}

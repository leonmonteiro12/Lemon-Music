package com.lemon.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingPlayerService extends Service {

    private static final String CHANNEL_ID =
            "lemon_music_floating";

    private static final int NOTIFICATION_ID =
            1001;

    // ==================================================
    // ABSOLUTE TINY PLAYER SIZE
    // ==================================================

    private static final int DEFAULT_WIDTH_DP =
            120;

    private static final int DEFAULT_HEIGHT_DP =
            80;

    private static final int MIN_WIDTH_DP =
            90;

    private static final int MIN_HEIGHT_DP =
            60;

    private static final int MAX_WIDTH_DP =
            430;

    private static final int MAX_HEIGHT_DP =
            320;

    private WindowManager windowManager;

    private View floatingView;

    private WebView webView;

    private WindowManager.LayoutParams params;

    private View topBar;

    private View resizeHandle;

    private int initialX;
    private int initialY;

    private float initialTouchX;
    private float initialTouchY;

    private int initialWidth;
    private int initialHeight;

    private boolean viewAdded = false;

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        createFloatingPlayer();
    }

    // ==================================================
    // NOTIFICATION
    // ==================================================

    private Notification createNotification() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            return new Notification.Builder(
                    this,
                    CHANNEL_ID
            )
                    .setContentTitle(
                            "Lemon Music 🍋"
                    )
                    .setContentText(
                            "Floating player is active"
                    )
                    .setSmallIcon(
                            android.R.drawable.ic_media_play
                    )
                    .setOngoing(true)
                    .build();

        } else {

            return new Notification.Builder(this)
                    .setContentTitle(
                            "Lemon Music 🍋"
                    )
                    .setContentText(
                            "Floating player is active"
                    )
                    .setSmallIcon(
                            android.R.drawable.ic_media_play
                    )
                    .setOngoing(true)
                    .build();
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Lemon Music Floating Player",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Lemon Music floating player"
            );

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    // ==================================================
    // FLOATING PLAYER
    // ==================================================

    private void createFloatingPlayer() {

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        if (windowManager == null) {

            stopSelf();

            return;
        }

        // ==================================================
        // ROOT
        // ==================================================

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.BLACK
        );

        // ==================================================
        // TOP BAR
        // ==================================================

        LinearLayout bar =
                new LinearLayout(this);

        topBar = bar;

        bar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bar.setBackgroundColor(
                Color.rgb(
                        22,
                        22,
                        22
                )
        );

        TextView title =
                new TextView(this);

        String savedTitle =
                getSharedPreferences(
                        "lemon_music_player",
                        MODE_PRIVATE
                )
                        .getString(
                                "video_title",
                                "🍋 Lemon Music"
                        );

        if (savedTitle == null ||
                savedTitle.trim().isEmpty()) {

            savedTitle =
                    "🍋 Lemon Music";
        }

        title.setText(
                savedTitle
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(
                9
        );

        title.setGravity(
                Gravity.CENTER_VERTICAL
        );

        title.setMaxLines(1);

        title.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        title.setPadding(
                dp(5),
                0,
                dp(1),
                0
        );

        bar.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(24),
                        1
                )
        );

        // ==================================================
        // CLOSE
        // ==================================================

        ImageButton close =
                new ImageButton(this);

        close.setImageResource(
                android.R.drawable
                        .ic_menu_close_clear_cancel
        );

        close.setColorFilter(
                Color.WHITE
        );

        close.setBackgroundColor(
                Color.TRANSPARENT
        );

        close.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        close.setContentDescription(
                "Close floating player"
        );

        close.setOnClickListener(
                v -> stopSelf()
        );

        bar.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(24),
                        dp(24)
                )
        );

        root.addView(
                bar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(24)
                )
        );

        // ==================================================
        // WEBVIEW
        // ==================================================

        webView =
                new WebView(this);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(
                true
        );

        settings.setDomStorageEnabled(
                true
        );

        settings.setDatabaseEnabled(
                true
        );

        settings.setMediaPlaybackRequiresUserGesture(
                false
        );

        settings.setBuiltInZoomControls(
                false
        );

        settings.setDisplayZoomControls(
                false
        );

        settings.setSupportZoom(
                false
        );

        settings.setLoadWithOverviewMode(
                true
        );

        settings.setUseWideViewPort(
                true
        );

        settings.setAllowFileAccess(
                true
        );

        settings.setAllowContentAccess(
                true
        );

        webView.setBackgroundColor(
                Color.BLACK
        );

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        // ==================================================
        // LOAD SAVED VIDEO
        // ==================================================

        String videoId =
                getSharedPreferences(
                        "lemon_music_player",
                        MODE_PRIVATE
                )
                        .getString(
                                "video_id",
                                ""
                        );

        if (videoId != null &&
                !videoId.trim().isEmpty()) {

            videoId =
                    videoId.trim();

            String embedUrl =
                    "https://www.youtube.com/embed/"
                            + videoId
                            + "?autoplay=1"
                            + "&playsinline=1"
                            + "&rel=0"
                            + "&modestbranding=1"
                            + "&enablejsapi=1"
                            + "&controls=1";

            loadEmbeddedHtml(
                    embedUrl
            );

        } else {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "No song selected 🍋"
            );

            empty.setTextColor(
                    Color.WHITE
            );

            empty.setTextSize(
                    10
            );

            empty.setGravity(
                    Gravity.CENTER
            );

            root.addView(
                    empty,
                    new LinearLayout.LayoutParams(
                            -1,
                            0,
                            1
                    )
            );
        }

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        // ==================================================
        // RESIZE HANDLE
        // ==================================================

        TextView resize =
                new TextView(this);

        resizeHandle =
                resize;

        resize.setText(
                "◢"
        );

        resize.setTextColor(
                Color.WHITE
        );

        resize.setTextSize(
                11
        );

        resize.setGravity(
                Gravity.CENTER
        );

        resize.setBackgroundColor(
                Color.rgb(
                        35,
                        35,
                        35
                )
        );

        LinearLayout.LayoutParams resizeParams =
                new LinearLayout.LayoutParams(
                        dp(20),
                        dp(18)
                );

        resizeParams.gravity =
                Gravity.END;

        root.addView(
                resize,
                resizeParams
        );

        floatingView =
                root;

        // ==================================================
        // WINDOW TYPE
        // ==================================================

        int windowType;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            windowType =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            windowType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        params =
                new WindowManager.LayoutParams(
                        dp(DEFAULT_WIDTH_DP),
                        dp(DEFAULT_HEIGHT_DP),
                        windowType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP |
                        Gravity.START;

        params.x =
                dp(10);

        params.y =
                dp(80);

        // ==================================================
        // DRAGGING
        // ==================================================

        bar.setOnTouchListener(
                new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

                        switch (
                                event.getAction()
                        ) {

                            case MotionEvent.ACTION_DOWN:

                                initialX =
                                        params.x;

                                initialY =
                                        params.y;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                params.x =
                                        initialX +
                                                (int) (
                                                        event.getRawX()
                                                                - initialTouchX
                                                );

                                params.y =
                                        initialY +
                                                (int) (
                                                        event.getRawY()
                                                                - initialTouchY
                                                );

                                updateWindow();

                                return true;

                            case MotionEvent.ACTION_UP:

                                return true;
                        }

                        return false;
                    }
                }
        );

        // ==================================================
        // RESIZING
        // ==================================================

        resize.setOnTouchListener(
                new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

                        switch (
                                event.getAction()
                        ) {

                            case MotionEvent.ACTION_DOWN:

                                initialWidth =
                                        params.width;

                                initialHeight =
                                        params.height;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                int deltaX =
                                        (int) (
                                                event.getRawX()
                                                        - initialTouchX
                                        );

                                int deltaY =
                                        (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );

                                int newWidth =
                                        initialWidth +
                                                deltaX;

                                int newHeight =
                                        initialHeight +
                                                deltaY;

                                int minWidth =
                                        dp(MIN_WIDTH_DP);

                                int minHeight =
                                        dp(MIN_HEIGHT_DP);

                                int maxWidth =
                                        dp(MAX_WIDTH_DP);

                                int maxHeight =
                                        dp(MAX_HEIGHT_DP);

                                newWidth =
                                        Math.max(
                                                minWidth,
                                                Math.min(
                                                        maxWidth,
                                                        newWidth
                                                )
                                        );

                                newHeight =
                                        Math.max(
                                                minHeight,
                                                Math.min(
                                                        maxHeight,
                                                        newHeight
                                                )
                                        );

                                params.width =
                                        newWidth;

                                params.height =
                                        newHeight;

                                updateWindow();

                                return true;

                            case MotionEvent.ACTION_UP:

                                return true;
                        }

                        return false;
                    }
                }
        );

        // ==================================================
        // ADD WINDOW
        // ==================================================

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M) {

                if (!Settings.canDrawOverlays(
                        this
                )) {

                    stopSelf();

                    return;
                }
            }

            windowManager.addView(
                    floatingView,
                    params
            );

            viewAdded =
                    true;

        } catch (Exception e) {

            viewAdded =
                    false;

            stopSelf();
        }
    }

    // ==================================================
    // YOUTUBE EMBED HTML
    // ==================================================

    private void loadEmbeddedHtml(
            String embedUrl
    ) {

        if (webView == null ||
                embedUrl == null ||
                embedUrl.isEmpty()) {

            return;
        }

        String html =
                "<!DOCTYPE html>"
                        + "<html>"
                        + "<head>"
                        + "<meta name=\"viewport\" "
                        + "content=\"width=device-width,"
                        + "initial-scale=1.0,"
                        + "maximum-scale=1.0,"
                        + "user-scalable=no\">"
                        + "<style>"
                        + "html,body{"
                        + "margin:0;"
                        + "padding:0;"
                        + "width:100%;"
                        + "height:100%;"
                        + "background:#000;"
                        + "overflow:hidden;"
                        + "}"
                        + "iframe{"
                        + "border:0;"
                        + "width:100%;"
                        + "height:100%;"
                        + "display:block;"
                        + "}"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<iframe "
                        + "src=\""
                        + escapeHtml(embedUrl)
                        + "\" "
                        + "allow=\"autoplay; encrypted-media; "
                        + "picture-in-picture; web-share\" "
                        + "allowfullscreen>"
                        + "</iframe>"
                        + "</body>"
                        + "</html>";

        /*
         * IMPORTANT:
         * Keep the existing 152-4 workaround.
         */
        webView.loadDataWithBaseURL(
                "https://lemonmusic.app/",
                html,
                "text/html",
                "UTF-8",
                "https://lemonmusic.app/"
        );
    }

    private String escapeHtml(
            String value
    ) {

        return value
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "\"",
                        "&quot;"
                );
    }

    // ==================================================
    // WINDOW UPDATE
    // ==================================================

    private void updateWindow() {

        if (!viewAdded ||
                windowManager == null ||
                floatingView == null ||
                params == null) {

            return;
        }

        try {

            windowManager.updateViewLayout(
                    floatingView,
                    params
            );

        } catch (Exception ignored) {
        }
    }

    // ==================================================
    // DP
    // ==================================================

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ==================================================
    // DESTROY
    // ==================================================

    @Override
    public void onDestroy() {

        viewAdded =
                false;

        if (webView != null) {

            try {
                webView.stopLoading();
            } catch (Exception ignored) {
            }

            try {
                webView.loadUrl(
                        "about:blank"
                );
            } catch (Exception ignored) {
            }

            try {
                webView.clearHistory();
            } catch (Exception ignored) {
            }

            try {
                webView.removeAllViews();
            } catch (Exception ignored) {
            }

            try {
                webView.destroy();
            } catch (Exception ignored) {
            }

            webView =
                    null;
        }

        if (windowManager != null &&
                floatingView != null) {

            try {

                windowManager.removeView(
                        floatingView
                );

            } catch (Exception ignored) {
            }

            floatingView =
                    null;
        }

        super.onDestroy();
    }

    // ==================================================
    // SERVICE BINDING
    // ==================================================

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
package com.lemon.music;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import java.util.Arrays;
import java.util.List;

public class YouTubeAuthManager {

    // =========================================================
    // PREFS
    // =========================================================

    public static final String YOUTUBE_PREFS =
            "lemon_music_youtube";

    public static final String ACCESS_TOKEN =
            "access_token";

    public static final String CONNECTED =
            "youtube_connected";

    public static final String ACCOUNT_EMAIL =
            "account_email";

    public static final String ACCOUNT_NAME =
            "account_name";

    // =========================================================
    // AUTH
    // =========================================================

    private static final String YOUTUBE_SCOPE =
            "https://www.googleapis.com/auth/youtube.readonly";

    /*
     * Used by Activity result handling if Google needs
     * additional user interaction.
     */
    public static final int REQUEST_AUTHORIZATION =
            9001;

    private final Activity activity;

    private final AuthorizationClient authorizationClient;

    private final List<Scope> requestedScopes =
            Arrays.asList(
                    new Scope(YOUTUBE_SCOPE)
            );

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public YouTubeAuthManager(
            @NonNull Activity activity
    ) {

        this.activity = activity;

        authorizationClient =
                Identity.getAuthorizationClient(
                        activity
                );
    }

    // =========================================================
    // NORMAL USER LOGIN
    // =========================================================

    public void authorize() {

        LemonDebug.log(
                "YouTubeAuth",
                "Starting YouTube authorization"
        );

        AuthorizationRequest request =
                AuthorizationRequest
                        .builder()
                        .setRequestedScopes(
                                requestedScopes
                        )
                        .build();

        authorizationClient
                .authorize(request)
                .addOnSuccessListener(
                        result -> {

                            LemonDebug.log(
                                    "YouTubeAuth",
                                    "Authorization request returned"
                            );

                            handleAuthorizationResult(
                                    result
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            LemonDebug.error(
                                    "YouTubeAuth",
                                    "Authorization request failed",
                                    error
                            );
                        }
                );
    }

    // =========================================================
    // SILENT / AUTOMATIC TOKEN REFRESH
    // =========================================================

    /**
     * Checks whether Google already has an authorization
     * grant for Lemon Music.
     *
     * This is intentionally NOT treated as a login screen.
     *
     * If Google can provide a fresh access token, we save it.
     *
     * If Google requires user interaction or temporarily fails,
     * we DO NOT sign the user out.
     */
    public void refreshAuthorization() {

        SharedPreferences prefs =
                getPrefs(activity);

        boolean connected =
                prefs.getBoolean(
                        CONNECTED,
                        false
                );

        if (!connected) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Silent refresh skipped: YouTube is not connected"
            );

            return;
        }

        LemonDebug.log(
                "YouTubeAuth",
                "Checking existing YouTube authorization"
        );

        AuthorizationRequest request =
                AuthorizationRequest
                        .builder()
                        .setRequestedScopes(
                                requestedScopes
                        )
                        .build();

        authorizationClient
                .authorize(request)
                .addOnSuccessListener(
                        result -> {

                            if (result == null) {

                                LemonDebug.log(
                                        "YouTubeAuth",
                                        "Silent refresh returned null result"
                                );

                                return;
                            }

                            if (result.hasResolution()) {

                                LemonDebug.log(
                                        "YouTubeAuth",
                                        "Silent refresh needs user interaction; keeping existing session"
                                );

                                /*
                                 * IMPORTANT:
                                 *
                                 * DO NOT clear the account here.
                                 *
                                 * The user did not press Sign Out.
                                 */
                                return;
                            }

                            saveAuthorizationResult(
                                    result
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            LemonDebug.error(
                                    "YouTubeAuth",
                                    "Silent authorization refresh failed; keeping existing session",
                                    error
                            );

                            /*
                             * IMPORTANT:
                             *
                             * Never clear ACCESS_TOKEN or CONNECTED here.
                             *
                             * A network error, temporary Google
                             * failure, or Play Services problem
                             * is NOT a user logout.
                             */
                        }
                );
    }

    // =========================================================
    // ACTIVITY RESULT
    // =========================================================

    public void handleActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        if (requestCode != REQUEST_AUTHORIZATION) {
            return;
        }

        LemonDebug.log(
                "YouTubeAuth",
                "Authorization activity result received"
        );

        if (resultCode != Activity.RESULT_OK) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Authorization activity was cancelled"
            );

            return;
        }

        if (data == null) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Authorization returned null intent"
            );

            return;
        }

        try {

            AuthorizationResult result =
                    authorizationClient
                            .getAuthorizationResultFromIntent(
                                    data
                            );

            saveAuthorizationResult(
                    result
            );

        } catch (ApiException e) {

            LemonDebug.error(
                    "YouTubeAuth",
                    "Could not read authorization result",
                    e
            );
        }
    }

    // =========================================================
    // PROCESS AUTH RESULT
    // =========================================================

    private void handleAuthorizationResult(
            AuthorizationResult result
    ) {

        if (result == null) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Authorization result was null"
            );

            return;
        }

        /*
         * Google requires additional user interaction.
         */
        if (result.hasResolution()) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Authorization requires user interaction"
            );

            try {

                result
                        .getPendingIntent()
                        .send();

            } catch (Exception e) {

                LemonDebug.error(
                        "YouTubeAuth",
                        "Could not launch authorization resolution",
                        e
                );
            }

            return;
        }

        saveAuthorizationResult(
                result
        );
    }

    // =========================================================
    // SAVE AUTHORIZATION RESULT
    // =========================================================

    private void saveAuthorizationResult(
            AuthorizationResult result
    ) {

        if (result == null) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Cannot save null authorization result"
            );

            return;
        }

        if (result.hasResolution()) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Authorization still requires resolution"
            );

            return;
        }

        String accessToken =
                result.getAccessToken();

        if (accessToken == null ||
                accessToken.trim().isEmpty()) {

            LemonDebug.log(
                    "YouTubeAuth",
                    "Google returned no access token"
            );

            /*
             * DO NOT mark the account as signed out.
             */
            return;
        }

        getPrefs(activity)
                .edit()
                .putString(
                        ACCESS_TOKEN,
                        accessToken
                )
                .putBoolean(
                        CONNECTED,
                        true
                )
                .apply();

        LemonDebug.log(
                "YouTubeAuth",
                "Fresh YouTube access token saved successfully"
        );
    }

    // =========================================================
    // SIGN OUT
    // =========================================================

    /**
     * Explicit user-requested sign out.
     *
     * This is the ONLY normal path that clears the
     * Lemon Music YouTube session.
     */
    public void signOut(
            Runnable onComplete
    ) {

        LemonDebug.log(
                "YouTubeAuth",
                "User requested YouTube sign out"
        );

        /*
         * Clear Lemon Music's local session first.
         *
         * This guarantees that pressing Sign Out actually
         * signs the account out locally even if Google
         * cannot complete the revoke request because of
         * network/Play Services issues.
         */
        clearLocalSession();

        /*
         * Ask Google to revoke the authorization grant.
         *
         * We intentionally don't depend on this succeeding
         * before clearing the local session.
         */
        try {

            authorizationClient
                    .revokeAccess(
                            com.google.android.gms.auth.api.identity.RevokeAccessRequest
                                    .builder()
                                    .setScopes(
                                            requestedScopes
                                    )
                                    .build()
                    )
                    .addOnSuccessListener(
                            unused -> {

                                LemonDebug.log(
                                        "YouTubeAuth",
                                        "Google authorization revoked"
                                );

                                runComplete(
                                        onComplete
                                );
                            }
                    )
                    .addOnFailureListener(
                            error -> {

                                LemonDebug.error(
                                        "YouTubeAuth",
                                        "Google authorization revoke failed",
                                        error
                                );

                                /*
                                 * Local session is already cleared.
                                 */
                                runComplete(
                                        onComplete
                                );
                            }
                    );

        } catch (Exception e) {

            LemonDebug.error(
                    "YouTubeAuth",
                    "Could not start Google authorization revoke",
                    e
            );

            runComplete(
                    onComplete
            );
        }
    }

    // =========================================================
    // CLEAR LOCAL SESSION
    // =========================================================

    private void clearLocalSession() {

        getPrefs(activity)
                .edit()
                .remove(
                        ACCESS_TOKEN
                )
                .remove(
                        ACCOUNT_EMAIL
                )
                .remove(
                        ACCOUNT_NAME
                )
                .putBoolean(
                        CONNECTED,
                        false
                )
                .apply();

        LemonDebug.log(
                "YouTubeAuth",
                "Local YouTube session cleared"
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void runComplete(
            Runnable onComplete
    ) {

        if (onComplete == null) {
            return;
        }

        activity.runOnUiThread(
                onComplete
        );
    }

    private static SharedPreferences getPrefs(
            Context context
    ) {

        return context.getSharedPreferences(
                YOUTUBE_PREFS,
                Context.MODE_PRIVATE
        );
    }

    // =========================================================
    // STATIC ACCESSORS
    // =========================================================

    public static String getSavedAccessToken(
            Context context
    ) {

        return getPrefs(
                context
        ).getString(
                ACCESS_TOKEN,
                ""
        );
    }

    public static boolean isYouTubeConnected(
            Context context
    ) {

        SharedPreferences prefs =
                getPrefs(
                        context
                );

        boolean connected =
                prefs.getBoolean(
                        CONNECTED,
                        false
                );

        String token =
                prefs.getString(
                        ACCESS_TOKEN,
                        ""
                );

        /*
         * Keep the existing behavior:
         * connected requires both the flag and a token.
         */
        return connected &&
                token != null &&
                !token.trim().isEmpty();
    }

    public static void clearSavedToken(
            Context context
    ) {

        getPrefs(
                context
        )
        .edit()
        .remove(
                ACCESS_TOKEN
        )
        .putBoolean(
                CONNECTED,
                false
        )
        .apply();

        LemonDebug.log(
                "YouTubeAuth",
                "Saved YouTube access token cleared"
        );
    }
}
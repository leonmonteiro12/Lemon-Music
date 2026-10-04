package com.lemon.music;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class aboutActivity extends Activity {

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_about
        );

        TextView aboutText =
                findViewById(
                        R.id.aboutText
                );

        Button back =
                findViewById(
                        R.id.buttonAboutBack
                );

        aboutText.setText(
                "🍋 LEMON MUSIC V7.0\n\n" +

                "Welcome to Lemon Music.\n" +
                "Because apparently making a normal " +
                "music player wasn't chaotic enough. 💀\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🎵 WHAT IS THIS THING?\n\n" +

                "Lemon Music is a personal music player " +
                "built by one developer who looked at " +
                "existing music apps and thought:\n\n" +

                "\"Yeah... I can make this worse.\"\n\n" +

                "And somehow... it worked. 🍋\n\n" +

                "It combines local music, YouTube-powered " +
                "music playback, playlists, background " +
                "playback and a bunch of other stuff " +
                "that definitely wasn't supposed to take " +
                "this long to build.\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🔥 FEATURES\n\n" +

                "🎵 YouTube music playback\n" +
                "📂 Local music library\n" +
                "📋 Playlists\n" +
                "⏭️ Next / Previous controls\n" +
                "⏪ 10-second rewind\n" +
                "⏩ 10-second forward\n" +
                "🎚️ Seek controls\n" +
                "🔔 Playback notification\n" +
                "🎧 Background playback\n" +
                "🔒 Lock-screen playback controls\n" +
                "🪟 Floating player\n" +
                "🛠️ Dedicated debug/logger system\n" +
                "🍋 Excessive amounts of lemon energy\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🧪 NEWPIPE POWERED\n\n" +

                "Lemon Music uses NewPipe for YouTube " +
                "stream extraction.\n\n" +

                "Instead of keeping a giant WebView alive " +
                "just to play audio, NewPipe obtains the " +
                "audio stream and Lemon Music hands it over " +
                "to the playback service.\n\n" +

                "Translation:\n\n" +

                "WebView: \"Please don't close me 😭\"\n\n" +
                "NewPipe: \"I got the audio. Move.\"\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🚀 BACKGROUND PLAYBACK\n\n" +

                "Once playback has been handed to the " +
                "LemonPlaybackService, the music can keep " +
                "playing even when Lemon Music leaves " +
                "the foreground.\n\n" +

                "Close the player screen?\n" +
                "Music keeps playing.\n\n" +

                "Go to the home screen?\n" +
                "Music keeps playing.\n\n" +

                "Open another app?\n" +
                "Music keeps playing.\n\n" +

                "Phone running Android 6?\n" +
                "We're still trying. 🍋💀\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🔔 NOTIFICATION CONTROLS\n\n" +

                "The playback notification gives you " +
                "quick access to playback controls without " +
                "having to reopen the entire app.\n\n" +

                "Because nobody wants to open an app just " +
                "to press pause.\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🪟 FLOATING PLAYER\n\n" +

                "Yes, the floating player is staying.\n\n" +

                "We are NOT deleting it.\n\n" +

                "It exists because sometimes you want your " +
                "music player floating around the screen " +
                "like it pays rent there.\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "🛠️ THE DEBUG SYSTEM\n\n" +

                "Lemon Music has its own dedicated logger " +
                "because watching Logcat explode while " +
                "trying to find one tiny error is not " +
                "exactly peak developer happiness.\n\n" +

                "The logger lives separately from the main " +
                "UI, because your homescreen does NOT need " +
                "to look like a nuclear reactor control panel.\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "📚 PLAYLISTS\n\n" +

                "Playlist support is being built so you can " +
                "actually move between tracks without the " +
                "app having an existential crisis every time " +
                "the next song button is pressed.\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "👨‍💻 DEVELOPER DEPARTMENT\n\n" +

                "Built by:\n" +
                "The Lemon himself 🍋\n\n" +

                "Design department:\n" +
                "Probably asleep.\n\n" +

                "Testing department:\n" +
                "The developer's phone.\n\n" +

                "QA department:\n" +
                "Also the developer's phone.\n\n" +

                "Customer support:\n" +
                "Me. Unfortunately.\n\n" +

                "Bug department:\n" +
                "Very well funded.\n\n" +
                
                "━━━━━━━━━━━━━━━━━━━━\n\n" +

"⚔️ THE KOTLIN FINAL BOSS FIGHT\n\n" +

"V7.0 also came with the legendary Kotlin Final Boss Fight. 💀\n\n" +

"Two entire days were spent fighting Kotlin errors, " +
"Gradle problems, dependency nonsense, JVM arguments, " +
"Android 6 compatibility and code that apparently " +
"decided it no longer wanted to exist.\n\n" +

"Every time one error was fixed:\n" +
"Kotlin: \"Congratulations. Here's another one.\"\n\n" +

"Two days later...\n\n" +

"Developer: \"I HAVE DEFEATED YOU.\"\n\n" +

"Kotlin:\n" +
"\"We'll see about that.\"\n\n" +

"🏆 FINAL BOSS STATUS: DEFEATED\n" +
"⏱️ Battle duration: 2 DAYS\n" +
"🧠 Brain cells remaining: Questionable (none are left)\n" +
"🍋 Lemon energy consumed: Unreasonably indefinite \n\n" +

"━━━━━━━━━━━━━━━━━━━━\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "💀 IMPORTANT NOTICE\n\n" +

                "This application was made with Java, " +
                "Android Studio alternatives, questionable " +
                "decisions, excessive debugging and an " +
                "unreasonable amount of determination.\n\n" +

                "If something works:\n" +
                "🎉 LET'S GOOOOO\n\n" +

                "If something breaks:\n" +
                "🗿 \"Interesting.\"\n\n" +

                "If something breaks only on Android 6:\n" +
                "💀💀💀\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "💰 DONATE TO THE CAUSE\n\n" +

                "Donate so the dev may pass his exams. 😭🙏\n\n" +

                "Your donation may be converted into:\n" +
                "📚 Study material\n" +
                "☕ Emergency developer fuel\n" +
                "🍜 Developer food\n" +
                "🧠 Remaining brain cells\n" +
                "💻 More Lemon Music development\n\n" +

                "No promises that the developer will " +
                "actually study with it though. 💀\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "❤️ THANK YOU\n\n" +

                "Thanks for using Lemon Music, testing it, " +
                "finding bugs, reporting bugs, and generally " +
                "helping this tiny lemon-powered project " +
                "survive.\n\n" +

                "Every version gets a little better.\n" +
                "Every bug gets a little angrier.\n\n" +

                "And somehow...\n" +

                "the lemon keeps developing. 🍋\n\n" +

                "━━━━━━━━━━━━━━━━━━━━\n\n" +

                "Made by the Lemon himself 🍋\n\n" +

                "LEMON MUSIC V7.0\n" +
                "More music. More chaos. More lemons."
        );

        back.setOnClickListener(
                v -> finish()
        );
    }
}
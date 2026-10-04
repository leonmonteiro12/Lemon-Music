package com.lemon.music;

public final class NewPipeAudioExtractor {

    private NewPipeAudioExtractor() {
        // Compatibility wrapper.
    }

    public interface Callback {

        void onSuccess(
                String audioUrl,
                String extractedTitle,
                String extractedArtist
        );

        void onError(
                String message
        );
    }

    public static void extract(
            String videoId,
            Callback callback
    ) {

        if (callback == null) {
            return;
        }

        /*
         * V7:
         *
         * The old queue code expects a NewPipe extractor.
         * For now we route that extraction through the
         * integrated InnerTune player backend.
         *
         * This keeps the existing playback/service code
         * untouched while giving queue playback the same
         * direct audio-stream extraction path.
         */

        InnerTuneAudioExtractor.extract(
                videoId,
                new InnerTuneAudioExtractor.Callback() {

                    @Override
                    public void onSuccess(
                            String audioUrl,
                            String extractedTitle,
                            String extractedArtist) {

                        callback.onSuccess(
                                audioUrl,
                                extractedTitle,
                                extractedArtist
                        );
                    }

                    @Override
                    public void onError(
                            String message) {

                        callback.onError(
                                message
                        );
                    }
                }
        );
    }
}
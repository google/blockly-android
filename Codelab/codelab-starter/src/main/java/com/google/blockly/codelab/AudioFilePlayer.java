package com.google.blockly.codelab;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A polyphonic audio file player with with an asynchronous callback for each file played.
 */
public class AudioFilePlayer {
    private static final String TAG = "AudioFilePlayer";

    public interface PlaySoundCallback {
        void onComplete();
        void onError(Throwable e);
    }

    private final Context mContext;

    /** Map of filenames to MediaPlayers that have the filename loaded but are not yet playing. */
    private Map<String, ArrayList<MediaPlayer>> mInactivePlayers = new ArrayMap<>();
    /** List of all active MediaPlayers. */
    private List<MediaPlayer> mActivePlayers = new ArrayList<>();

    /** Audio files located in the <code>assets/sounds/</code> directory. */
    public static List<String> AUDIO_FILES = Collections.unmodifiableList(Arrays.asList(
            "c4.m4a",
            "d4.m4a",
            "e4.m4a",
            "f4.m4a",
            "g4.m4a",
            "a5.m4a",
            "b5.m4a",
            "c5.m4a",
            "horn.wav",
            "success.wav",
            "whistle.wav",
            "yeah.mp3"
    ));

    public AudioFilePlayer(Context context) {
        mContext = context;

        // Maybe prepare one of each audio files (as long as there aren't too many.
    }

    /**
     * Plays the named audio file, with a callback notifying when the file is complete.
     * @param filename The name of the file found in <code>assets/sounds/</code>.
     * @param callback Called when the audio file completes, or the file loading errors.
     */
    public void playSound(final String filename, @Nullable final PlaySoundCallback callback) {
        try {
            findOrCreatePlayer(filename,
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer player) {
                            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer player) {
                                    mActivePlayers.remove(player);

                                    if (callback != null) {
                                        callback.onComplete();
                                    }

                                    mInactivePlayers.get(filename).add(player);
                                }
                            });
                            player.start();

                            mActivePlayers.add(player);
                        }
                    });
        } catch (IOException e) {
            if (callback != null) {
                callback.onError(e);
            } else {
                Log.e(TAG, "Failed to load file \"" + filename + "\"", e);
            }
        }
    }

    /**
     * Stops all audio and releases all MediaPlayers.
     */
    public void stop() {
        for (MediaPlayer player : mActivePlayers) {
            player.stop();
            player.release();
        }
        mActivePlayers.clear();

        for (String filename: mInactivePlayers.keySet()) {
            for (MediaPlayer player : mInactivePlayers.get(filename)) {
                player.release();
            }
        }
        mInactivePlayers.clear();
    }

    /**
     * Finds an inactive MediaPlayer with the requested file, possibly creating a new one.
     * @param filename The audio file to load from <code>assets/sounds/</code>.
     * @param callback The callback when the audio file is ready.
     * @throws IOException If the file fails to load.
     */
    private void findOrCreatePlayer(
            final String filename, @Nullable final MediaPlayer.OnPreparedListener callback)
            throws IOException {
        ArrayList<MediaPlayer> list = mInactivePlayers.get(filename);
        if (list == null) {
            list = new ArrayList<>();
            mInactivePlayers.put(filename, list);
        }

        if (list.isEmpty()) {
            MediaPlayer player = new MediaPlayer();
            AssetFileDescriptor afd = mContext.getAssets().openFd("sounds/" + filename);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    if (callback != null) {
                        mActivePlayers.add(player);
                        callback.onPrepared(player);
                    } else {
                        mInactivePlayers.get(filename).add(player);
                    }
                }
            });
            player.prepareAsync();
        } else {
            if (callback != null) {
                // Pop the last one off the list.
                callback.onPrepared(list.remove(list.size() - 1));
            }
        }
    }
}

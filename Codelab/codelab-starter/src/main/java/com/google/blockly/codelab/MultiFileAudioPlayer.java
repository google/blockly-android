package com.google.blockly.codelab;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.io.IOException;

/**
 * A polyphonic audio file player with with an asynchronous callback for each file played.
 * Only supports a preselected set of files with known, hardcoded durations
 */
public class MultiFileAudioPlayer {
    private static final String TAG = "AudioFilePlayer";

    /**
     * The effective duration of the piano not audio files.
     * The actual durations of the notes range from 718 to 918 milliseconds.
     * However, to implement a good cadence, we'll treat all of them as having
     * the same duration. We'll even treat them as shorter to give overlap.
     */
    private static final Long NOTE_MILLISECONDS = 660L;

    public interface PlaySoundCallback {
        void onComplete(SoundFile soundFile);
        void onError(SoundFile soundFile);
    }

    private final Context mContext;
    private final Handler mMainHandler;
    private final SoundPool mSoundPool;

    /** Audio files located in the <code>assets/sounds/</code> directory. */
    public final ArrayMap<String, SoundFile> mSoundFiles = new ArrayMap<>();

    public MultiFileAudioPlayer(Context context) {
        mContext = context;
        mMainHandler = new Handler(context.getMainLooper());

        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttrs)
                .setMaxStreams(9)  // One per button
                .build();

        mSoundFiles.put("C4", new SoundFile("C4", "c4.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("D4", new SoundFile("D4", "d4.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("E4", new SoundFile("E4", "e4.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("F4", new SoundFile("F4", "f4.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("G4", new SoundFile("G4", "g4.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("A5", new SoundFile("A5", "a5.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("B5", new SoundFile("B5", "b5.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("C5", new SoundFile("C5", "c5.m4a", NOTE_MILLISECONDS));
        mSoundFiles.put("Horn", new SoundFile("Horn", "horn.wav", 707L));
        mSoundFiles.put("Success", new SoundFile("Success", "success.wav", 2275L));
        mSoundFiles.put("Whistle", new SoundFile("Whistle", "whistle.wav", 928L));
        mSoundFiles.put("Yeah!", new SoundFile("Yeah!", "yeah.mp3", 1175L));
    }

    /**
     * Plays the named audio file, with a callback notifying when the file is complete.
     * @param id The identifying short name of the sound file.
     * @param callback Called when the audio file completes, or the file loading errors.
     */
    public void playSound(final String id, @Nullable final PlaySoundCallback callback) {
        final SoundFile sound = mSoundFiles.get(id);
        if (sound == null) {
            throw new IllegalArgumentException("Unknown file \"" + id + "\".");
        }
        if (!sound.play()) {
            if (callback != null) {
                callback.onError(sound);
            }
            return;
        }

        if (callback != null) {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onComplete(sound);
                }
            }, sound.duration);
        }
    }

    /**
     * Stops all audio and releases all MediaPlayers.
     */
    public void pause() {
        mSoundPool.autoPause();
    }

    public void resume() {
        mSoundPool.autoResume();
    }

    class SoundFile {
        final String shortName;
        final String filename;
        final long duration;
        private final int id;

        SoundFile(String shortName, String filename, long duration) {
            this.shortName = shortName;
            this.filename = filename;
            this.duration = duration;

            try {
                AssetFileDescriptor afd = mContext.getAssets().openFd("sounds/" + filename);
                this.id = mSoundPool.load(afd, 1);
                afd.close();
            } catch (IOException e) {
                throw new IllegalStateException("Error loading sound file \"" + filename + "\"", e);
            }
        }

        public boolean play() {
            return mSoundPool.play(id, 1f, 1f, 1, 0, 1f) != 0;
        }
    }
}

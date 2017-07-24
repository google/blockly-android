package com.google.blockly.codelab;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment to playback the scripts associated with each button.
 */
public class PlaybackFragment extends Fragment {
    private AudioFilePlayer mAudioFilePlayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioFilePlayer = new AudioFilePlayer(getContext());
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.buttons, null);

        // Each button plays a different file.
        rootView.findViewById(R.id.button1).setOnClickListener(buildPlayOnClick(1));
        rootView.findViewById(R.id.button2).setOnClickListener(buildPlayOnClick(2));
        rootView.findViewById(R.id.button3).setOnClickListener(buildPlayOnClick(3));
        rootView.findViewById(R.id.button4).setOnClickListener(buildPlayOnClick(4));
        rootView.findViewById(R.id.button5).setOnClickListener(buildPlayOnClick(5));
        rootView.findViewById(R.id.button6).setOnClickListener(buildPlayOnClick(6));
        rootView.findViewById(R.id.button7).setOnClickListener(buildPlayOnClick(7));
        rootView.findViewById(R.id.button8).setOnClickListener(buildPlayOnClick(8));
        rootView.findViewById(R.id.button9).setOnClickListener(buildPlayOnClick(9));

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_option_menu, menu);

        MenuItem edit = menu.findItem(R.id.action_edit_buttons);
        edit.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ((MainActivity) getActivity()).startSelectForEdit();
                return false;
            }
        });
    }

    @Override
    public void onStop() {
        mAudioFilePlayer.stop();
        super.onStop();
    }

    /**
     * Helper function to construct a new {@link View.OnClickListener} that plays
     * one of the {@link AudioFilePlayer}'s audio files.
     * @return A new View.OnClickListener.
     */
    private View.OnClickListener buildPlayOnClick(final int n) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioFilePlayer.playSound(AudioFilePlayer.AUDIO_FILES.get(n), null);
            }
        };
    }
}

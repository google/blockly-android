package com.google.blockly.codelab;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * The application's main (and only) activity, hosting the toolbar and content fragments.
 */
public class MainActivity extends AppCompatActivity {

    private ButtonsFragment buttonsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonsFragment = (ButtonsFragment)
                getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }
}

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.codelab;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * An array of colorful buttons, with some instructional text and an "Edit" menu option.
 */
public class EditFragment extends Fragment {
    private static final String TAG = "EditFragment";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.edit_fragment, null);

        // Each button edits a different script.
        rootView.findViewById(R.id.button1).setOnClickListener(buildEditOnClick(1));
        rootView.findViewById(R.id.button2).setOnClickListener(buildEditOnClick(2));
        rootView.findViewById(R.id.button3).setOnClickListener(buildEditOnClick(3));
        rootView.findViewById(R.id.button4).setOnClickListener(buildEditOnClick(4));
        rootView.findViewById(R.id.button5).setOnClickListener(buildEditOnClick(5));
        rootView.findViewById(R.id.button6).setOnClickListener(buildEditOnClick(6));
        rootView.findViewById(R.id.button7).setOnClickListener(buildEditOnClick(7));
        rootView.findViewById(R.id.button8).setOnClickListener(buildEditOnClick(8));
        rootView.findViewById(R.id.button9).setOnClickListener(buildEditOnClick(9));

        return rootView;
    }

    protected void onEdit(int buttonNumber) {
        // TODO: Edit sound script
        Log.d(TAG, "Button #" + buttonNumber);
    }

    protected View.OnClickListener buildEditOnClick(final int buttonNumber) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEdit(buttonNumber);
            }
        };
    }
}

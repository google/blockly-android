/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.blockly.model.Workspace;
import com.google.blockly.ui.VirtualWorkspaceView;
import com.google.blockly.ui.WorkspaceView;

/**
 * Fragment that holds the active workspace and its views.
 */
public class WorkspaceFragment extends Fragment {
    private static final String TAG = "WorkspaceFragment";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_BUILD_DEBUG_MODEL = "debug_model";
    private WorkspaceView mWorkspaceView;
    private Workspace mWorkspace;
    private View.OnClickListener mTrashClickListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mWorkspace == null) {
            throw new IllegalStateException(
                    "A Workspace must be set before this fragment's view is created.");
        }
        final ViewGroup rootView =
                (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);

        mWorkspaceView = (WorkspaceView) rootView.findViewById(R.id.workspace);

        final VirtualWorkspaceView virtualWorkspaceView =
                (VirtualWorkspaceView) rootView.findViewById(R.id.virtual_workspace);
        ImageButton trashButton = (ImageButton) rootView.findViewById(R.id.trash_button);
        mWorkspaceView.setTrashView(trashButton);
        if (mTrashClickListener != null) {
            trashButton.setOnClickListener(mTrashClickListener);
        }

        rootView.findViewById(R.id.reset_view_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        virtualWorkspaceView.resetView();
                    }
                });

        rootView.findViewById(R.id.zoom_out_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        virtualWorkspaceView.zoomOut();
                    }
                });

        rootView.findViewById(R.id.zoom_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        virtualWorkspaceView.zoomIn();
                    }
                });

        // Let the controller create the views.
        mWorkspace.initWorkspaceView(mWorkspaceView);
        return rootView;
    }

    /**
     * Sets the workspace to use in this fragment for instantiating views. This should be the same
     * workspace used for an associated {@link ToolboxFragment} or {@link TrashFragment}.
     *
     * @param workspace The workspace backing this fragment.
     */
    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
    }

    /**
     * @return The workspace being used by this fragment.
     */
    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public WorkspaceView getWorkspaceView() {
        return mWorkspaceView;
    }

    /**
     * Sets the listener to be called when the trash is clicked. This is generally used to open
     * the {@link TrashFragment}.
     *
     * @param listener The listener to call when the trash is clicked.
     */
    public void setTrashClickListener(View.OnClickListener listener) {
        mTrashClickListener = listener;
    }
}

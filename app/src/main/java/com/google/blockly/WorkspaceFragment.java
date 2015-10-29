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
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.blockly.model.BlockFactory;
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
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Workspace mWorkspace;
    private View.OnClickListener mTrashClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fragmentManager = getFragmentManager();
        mWorkspace = new Workspace();
        mWorkspace.setTrashFragment((TrashFragment) fragmentManager.findFragmentById(R.id.trash));
        BlockFactory blockFactory = new BlockFactory(getContext(), new int[]{R.raw.toolbox_blocks});
        mWorkspace.loadToolboxContents(getContext(), blockFactory, R.raw.toolbox);
        mWorkspace.setToolboxFragment(
                (ToolboxFragment) fragmentManager.findFragmentById(R.id.toolbox), getContext());
        final Bundle bundle = this.getArguments();
        if (bundle != null && bundle.containsKey(ARG_SECTION_NUMBER)) {
            // Add all blocks, or load from XML.
            MockBlocksProvider.makeTestModel(mWorkspace);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView =
                (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);

        final WorkspaceView workspaceView =
                (WorkspaceView) rootView.findViewById(R.id.workspace);
        workspaceView.setWorkspace(mWorkspace);

        final VirtualWorkspaceView virtualWorkspaceView =
                (VirtualWorkspaceView) rootView.findViewById(R.id.virtual_workspace);
        ImageButton trashButton = (ImageButton) rootView.findViewById(R.id.trash_button);
        workspaceView.setTrashView(trashButton);
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
        mWorkspace.createViewsFromModel(workspaceView, getActivity());
        return rootView;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public void setTrashClickListener(View.OnClickListener listener) {
        mTrashClickListener = listener;
    }

    /**
     * @param sectionNumber Which section's workspace to return.
     *
     * @return a new instance of this fragment for the given section
     * number.
     */
    public static WorkspaceFragment newInstance(int sectionNumber) {
        WorkspaceFragment fragment = new WorkspaceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }
}

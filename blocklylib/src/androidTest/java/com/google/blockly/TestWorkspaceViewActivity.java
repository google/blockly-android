package com.google.blockly;

import android.app.Activity;
import android.os.Bundle;

import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.Workspace;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;

/**
 * Minimal Activity hosting a Workspace, for testing Blockly Views.
 */
public class TestWorkspaceViewActivity extends Activity {
    public BlockFactory mBlockFactory;
    public WorkspaceHelper mWorkspaceHelper;
    public Workspace mWorkspace;
    public WorkspaceView mWorkspaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWorkspace = new Workspace.Builder(this).build();
        mWorkspaceHelper = mWorkspace.getWorkspaceHelper();

        mWorkspaceView = new WorkspaceView(this);
        mWorkspace.initWorkspaceView(mWorkspaceView);

        setContentView(mWorkspaceView);
    }
}

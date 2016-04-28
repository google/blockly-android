package com.google.blockly.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.google.blockly.android.R;

/**
 * A view containing a complete Blockly editor, including a workspace, a toolbox, and a trash.
 */
public class BlocklyUnifiedWorkspace extends RelativeLayout {
    public BlocklyUnifiedWorkspace(Context context) {
        super(context);
        onFinishInflate();
    }

    public BlocklyUnifiedWorkspace(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlocklyUnifiedWorkspace(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(getContext()).inflate(
                R.layout.blockly_unified_workspace_contents, this);

        findViewById(R.id.blockly_overlay_buttons).bringToFront();
    }
}

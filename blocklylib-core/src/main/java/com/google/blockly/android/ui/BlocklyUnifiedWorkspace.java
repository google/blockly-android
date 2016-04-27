package com.google.blockly.android.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.R;
import com.google.blockly.android.control.BlocklyController;

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

        final AbstractBlocklyActivity activity = (AbstractBlocklyActivity) getContext();
        new Handler(activity.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final BlocklyController controller = activity.getController();
                findViewById(R.id.zoom_in_button).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                controller.zoomIn();
                            }
                        });
                findViewById(R.id.zoom_out_button).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                controller.zoomOut();
                            }
                        });
                findViewById(R.id.center_view_button).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                controller.recenterWorkspace();
                            }
                        });
            }
        });
    }
}

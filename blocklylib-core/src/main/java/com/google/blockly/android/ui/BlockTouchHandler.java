/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui;

import android.view.MotionEvent;
import com.google.blockly.android.ui.fieldview.FieldView;

/**
 * Callback handle for touches to BlockViews.
 */
public abstract class BlockTouchHandler {
    /**
     * Called by the BlockView when the visible area of the block has been touched.
     *
     * @param blockView The touched {@link BlockView}.
     * @param motionEvent The event the blockView is responding to.
     * @return whether the {@link WorkspaceView} has handled the touch event.
     */
    abstract public boolean onTouchBlock(BlockView blockView, MotionEvent motionEvent);

    /**
     * Called by the BlockView when the visible area of the block or its child/descendent Views have
     * been touched. Facilitates with drags that begin on {@link FieldView}s.
     *
     * @param blockView The touched {@link BlockView}.
     * @param motionEvent The event the blockView is responding to.
     * @return whether the {@link WorkspaceView} has handled the touch event.
     */
    abstract public boolean onInterceptTouchEvent(BlockView blockView, MotionEvent motionEvent);
}

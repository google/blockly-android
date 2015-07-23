/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.blockly.blocks.Block;

/**
 * Created by epastern on 5/21/15.
 */
public class BlockView extends FrameLayout {
    public BlockView(Context context, AttributeSet attrs, Block block) {
        super(context, attrs);
    }
}

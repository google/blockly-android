/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.fieldview;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FieldLabel;

import org.mockito.Mock;

/**
 * Tests for {@link BasicFieldLabelView}.
 */
public class BasicFieldLabelViewTest extends MockitoAndroidTestCase {

    private static final String INIT_TEXT_VALUE = "someTextToInitializeLabel";

    // Cannot mock final classes.
    private FieldLabel mFieldLabel;

    // Verify object instantiation.
    public void testInstantiation() {
        mFieldLabel = new FieldLabel("FieldLabel", INIT_TEXT_VALUE);

        final BasicFieldLabelView view = new BasicFieldLabelView(getContext());
        view.setField(mFieldLabel);
        assertSame(mFieldLabel, view.getField());
        assertEquals(INIT_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }
}

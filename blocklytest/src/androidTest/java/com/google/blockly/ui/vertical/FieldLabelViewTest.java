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

package com.google.blockly.ui.vertical;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;

/**
 * Tests for {@link FieldLabelView}.
 */
public class FieldLabelViewTest extends MockitoAndroidTestCase {

    private static final String INIT_TEXT_VALUE = "someTextToInitializeLabel";

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    // Cannot mock final classes.
    private Field.FieldLabel mFieldLabel;

    // Verify object instantiation.
    public void testInstantiation() {
        mFieldLabel = new Field.FieldLabel("FieldLabel", INIT_TEXT_VALUE);
        assertNotNull(mFieldLabel);

        final FieldLabelView view =
                new FieldLabelView(getContext(), mFieldLabel, null);
        assertSame(view, mFieldLabel.getView());
        assertEquals(INIT_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }
}

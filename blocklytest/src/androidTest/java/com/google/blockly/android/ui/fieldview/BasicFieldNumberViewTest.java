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

import static com.google.common.truth.Truth.assertThat;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.model.FieldNumber;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link BasicFieldNumberView}.
 */
public class BasicFieldNumberViewTest extends BlocklyTestCase {

    private static final double INITIAL_POS_INT = 5d;
    private static final double INITIAL_SIGNED_INT = -5d;
    private static final double INITIAL_POS_DECIMAL = Math.PI;
    private static final double INITIAL_SIGNED_DECIMAL = -Math.PI;

    /**
     * A regex to match the decimal string equivalent of {@link Math#PI}, accounting for minor
     * platform variations. (Don't ask me why.)
     */
    private static final String PI_STRING_REGEX = "3\\.141592653589793?";


    // Cannot mock final classes.
    private FieldNumber mFieldPosInt;
    private FieldNumber mFieldPosDecimal;
    private FieldNumber mFieldSignedInt;
    private FieldNumber mFieldSignedDecimal;

    // Subject of tests.
    private BasicFieldNumberView view;

    @Before
    public void setUp() throws Exception {
        configureForUIThread();

        view = new BasicFieldNumberView(getContext());

        mFieldPosInt = new FieldNumber("POSITIVE_INTEGER");
        mFieldPosInt.setConstraints(0d, Double.NaN, 1d);   // min, max, precision
        mFieldPosInt.setValue(INITIAL_POS_INT);

        mFieldPosDecimal = new FieldNumber("POSITIVE_DECIMAL");
        mFieldPosDecimal.setConstraints(0d, Double.NaN, Double.NaN);
        mFieldPosDecimal.setValue(INITIAL_POS_DECIMAL);

        mFieldSignedInt = new FieldNumber("SIGNED_INTEGER");
        mFieldSignedInt.setConstraints(Double.NaN, Double.NaN, 1d);
        mFieldSignedInt.setValue(INITIAL_SIGNED_INT);

        mFieldSignedDecimal = new FieldNumber("SIGNED_DECIMAL");
        // Default constraints.
        assertThat(mFieldSignedDecimal.hasMinimum()).isFalse();
        assertThat(mFieldSignedDecimal.hasMaximum()).isFalse();
        assertThat(mFieldSignedDecimal.hasPrecision()).isFalse();
        mFieldSignedDecimal.setValue(INITIAL_SIGNED_DECIMAL);
    }

    /**
     * Verifies {@link BasicFieldNumberView#setField} updates the associated field and the text
     * value presented to the user.
     */
    @Test
    public void testSetField() {
        view.setField(mFieldPosInt);
        assertThat(view.getField()).isSameAs(mFieldPosInt);
        assertThat(view.getText().toString())
                .isEqualTo("5");

        view.setField(mFieldSignedInt);
        assertThat(view.getField()).isSameAs(mFieldSignedInt);
        assertThat(view.getText().toString())
                .isEqualTo("-5");

        // The following is the maximum precision presentation of PI with a double.
        view.setField(mFieldPosDecimal);
        assertThat(view.getField()).isSameAs(mFieldPosDecimal);
        assertThat(view.getText().toString()).matches(PI_STRING_REGEX);

        view.setField(mFieldSignedDecimal);
        assertThat(view.getField()).isSameAs(mFieldSignedDecimal);
        assertThat(view.getText().toString()).matches("-" + PI_STRING_REGEX);
    }
}

package com.google.blockly.ui.fieldview;

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
                new FieldLabelView(getContext(), mFieldLabel, mMockWorkspaceHelper);
        assertSame(view, mFieldLabel.getView());
        assertEquals(INIT_TEXT_VALUE, view.getText().toString());  // Fails without .toString()
    }
}

package com.google.blockly.ui.fieldview;

import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.google.blockly.MockitoAndroidTestCase;
import com.google.blockly.model.Field;
import com.google.blockly.ui.WorkspaceHelper;

import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Tests for {@link FieldVariableView}.
 */
public class FieldVariableViewTest extends MockitoAndroidTestCase {

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    private Field.FieldVariable mFieldVariable;
    private String[] mVariables = new String[] {"var1", "var2", "var3"};

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mFieldVariable = new Field.FieldVariable("FieldVariable", "Var2");
        assertNotNull(mFieldVariable);
        assertEquals("Var2", mFieldVariable.getVariable());
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final FieldVariableView view = makeFieldVariableView();
        assertSame(view, mFieldVariable.getView());
        assertEquals(mVariables.length, view.getCount());
        assertEquals(mFieldVariable.getVariable().toLowerCase(), view.getSelectedItem().toString());
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): need tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling FieldVariableView.setSelection().
    public void testUpdateFieldFromView() {
        final FieldVariableView view = makeFieldVariableView();

        view.setSelection(2);
        assertEquals(mVariables[2], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        view.setSelection(0);
        assertEquals(mVariables[0], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        view.setSelection(1);
        assertEquals(mVariables[1], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());
    }

    // Test update of view if variable selection changes.
    public void testUpdateViewFromField() {
        final FieldVariableView view = makeFieldVariableView();

        mFieldVariable.setVariable(mVariables[0]);
        assertEquals(mVariables[0], view.getSelectedItem().toString());

        mFieldVariable.setVariable(mVariables[1]);
        assertEquals(mVariables[1], view.getSelectedItem().toString());

        mFieldVariable.setVariable(mVariables[2]);
        assertEquals(mVariables[2], view.getSelectedItem().toString());
    }

    @NonNull
    private FieldVariableView makeFieldVariableView() {
        Mockito.when(mMockWorkspaceHelper.getVariablesAdapter())
                .thenReturn(new ArrayAdapter<String>(getContext(),
                        android.R.layout.simple_spinner_item, mVariables));
        return new FieldVariableView(getContext(), mFieldVariable, mMockWorkspaceHelper);
    }
}

package com.google.blockly.android.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.test.R;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class NameVariableDialogTest extends BlocklyTestCase {

  private NameVariableDialog mNameVariableDialogFragment;
  private BlocklyTestActivity mActivity;

  @Rule
  public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
      new ActivityTestRule<>(BlocklyTestActivity.class);

  @Before
  public void setUp() throws Exception {
    configureForUIThread();
    mNameVariableDialogFragment = new NameVariableDialog();

    mActivity = mActivityRule.getActivity();
  }

  @Test
  public void dialogShowsOldVariableNameWhenRenaming() throws Exception {
    String variableName = "oldVariableName";

    mNameVariableDialogFragment.setVariable(variableName, mock(NameVariableDialog.Callback.class), true);
    mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "RenameFragment");
    onView(withId(R.id.description)).check(matches(withText(new StringContains(variableName))));
  }

  @Test
  public void dialogShowsGenericTextForNewVariable() throws Exception {
    mNameVariableDialogFragment.setVariable("", mock(NameVariableDialog.Callback.class), false);
    mNameVariableDialogFragment.show(mActivity.getSupportFragmentManager(), "CreateFragment");
    onView(withId(R.id.description)).check(matches(withText("New variable name")));
  }
}

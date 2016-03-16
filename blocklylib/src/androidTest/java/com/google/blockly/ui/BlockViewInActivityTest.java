package com.google.blockly.ui;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewParent;

import com.google.blockly.R;
import com.google.blockly.TestWorkspaceViewActivity;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;


/**
 * {@link BlockView} tests in the context of an attached Activity.
 */
public class BlockViewInActivityTest
        extends ActivityInstrumentationTestCase2<TestWorkspaceViewActivity> {
    private TestWorkspaceViewActivity mActivity;
    private Instrumentation mInstrumentation;
    private BlockFactory mBlockFactory;

    // Block hierarchy, loaded from "controls_whileUntil".
    private ArrayList<Block> mBlocks;
    private Block mRootBlock;
    private Block mChildInputBlock;
    private Block mChildStatementBlock;
    private BlockView mRootView;
    private View mFieldView;
    private BlockView mChildInputBlockView;
    private BlockView mChildStatementBlockView;

    @Mock
    private ConnectionManager mMockConnectionManager;

    public BlockViewInActivityTest() {
        super(TestWorkspaceViewActivity.class);
    }

    @Override
    public void setUp() {
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();

        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", mActivity.getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);

        mBlockFactory = new BlockFactory(mActivity, new int[]{R.raw.test_blocks});
        mBlocks = new ArrayList<>();
    }

    public void loadWhileUtilBlocksIntoWorkspaceView() {
        mRootBlock = mBlockFactory.obtainBlock("controls_whileUntil", "1");
        assertNotNull(mRootBlock);
        mChildInputBlock = mBlockFactory.obtainBlock("output_no_input", "2");
        mRootBlock.getInputByName("TIMES").getConnection()
                .connect(mChildInputBlock.getOutputConnection());
        mChildStatementBlock = mBlockFactory.obtainBlock("statement_no_input", "3");
        mRootBlock.getInputByName("NAME").getConnection()
                .connect(mChildStatementBlock.getPreviousConnection());

        mActivity.mWorkspaceHelper.buildBlockGroupTree(mRootBlock, mMockConnectionManager, null);
        mRootView = mRootBlock.getView();
        mFieldView = (View) mRootBlock.getFieldByName("MODE").getView();
        mChildInputBlockView = mChildInputBlock.getView();
        mChildStatementBlockView = mChildStatementBlock.getView();

        BlockGroup rootBlockGroup = (BlockGroup) mRootView.getParent();
        mActivity.mWorkspaceView.addView(rootBlockGroup);
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    public void testActivatedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUtilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildInputBlockView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildStatementBlockView, mRootBlock.getView()));

        assertFalse(mRootView.isActivated());
        assertFalse(mFieldView.isActivated());
        assertFalse(mChildInputBlockView.isActivated());
        assertFalse(mChildStatementBlockView.isActivated());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRootView.setActivated(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be activated.
        assertTrue(mRootView.isActivated());
        assertFalse(mFieldView.isActivated());
        assertFalse(mChildInputBlockView.isActivated());
        assertFalse(mChildStatementBlockView.isActivated());
    }

    /** Tests that pressed {@link View} state does not propagate to child BlockViews. */
    public void testPressedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUtilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildInputBlockView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildStatementBlockView, mRootBlock.getView()));

        assertFalse(mRootView.isPressed());
        assertFalse(mFieldView.isPressed());
        assertFalse(mChildInputBlockView.isPressed());
        assertFalse(mChildStatementBlockView.isPressed());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRootView.setPressed(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be pressed.
        assertTrue(mRootView.isPressed());
        assertFalse(mFieldView.isPressed());
        assertFalse(mChildInputBlockView.isPressed());
        assertFalse(mChildStatementBlockView.isPressed());
    }

    /** Tests that focused {@link View} state does not propagate to child BlockViews. */
    public void testFocusedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUtilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildInputBlockView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildStatementBlockView, mRootBlock.getView()));

        assertFalse(mRootView.isFocused());
        assertFalse(mFieldView.isFocused());
        assertFalse(mChildInputBlockView.isFocused());
        assertFalse(mChildStatementBlockView.isFocused());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRootView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be focused.
        assertTrue(mRootView.isFocused());
        assertFalse(mFieldView.isFocused());
        assertFalse(mChildInputBlockView.isFocused());
        assertFalse(mChildStatementBlockView.isFocused());
    }

    /** Tests that selected {@link View} state does not propagate to child BlockViews. */
    public void testSelectedStateDoesNotAffectChildren() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadWhileUtilBlocksIntoWorkspaceView();
            }
        });
        mInstrumentation.waitForIdleSync();

        // Preconditions
        assertTrue(isDescendentOf(mFieldView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildInputBlockView, mRootBlock.getView()));
        assertTrue(isDescendentOf(mChildStatementBlockView, mRootBlock.getView()));

        assertFalse(mRootView.isSelected());
        assertFalse(mFieldView.isSelected());
        assertFalse(mChildInputBlockView.isSelected());
        assertFalse(mChildStatementBlockView.isSelected());

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRootView.setSelected(true);
            }
        });
        mInstrumentation.waitForIdleSync();

        // Only mRootView should be selected.
        assertTrue(mRootView.isSelected());
        assertFalse(mFieldView.isSelected());
        assertFalse(mChildInputBlockView.isSelected());
        assertFalse(mChildStatementBlockView.isSelected());
    }

    private static boolean isDescendentOf(View child, View ancestor) {
        assertNotNull(child);
        assertNotNull(ancestor);

        ViewParent parent = child.getParent();
        while (parent != null && parent instanceof View) {
            if (ancestor == parent) {
                return true;
            }
            parent = ((View) parent).getParent();
        }
        return false;
    }
}

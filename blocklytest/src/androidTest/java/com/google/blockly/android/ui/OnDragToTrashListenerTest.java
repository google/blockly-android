package com.google.blockly.android.ui;

import android.content.ClipDescription;
import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.clipboard.BlockClipDataHelper;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Unit tests for {@link OnDragToTrashListener}.
 */
public class OnDragToTrashListenerTest extends BlocklyTestCase {
    @Mock
    BlocklyController mMockController;
    @Mock
    BlockClipDataHelper mMockClipDataHelper;

    @Mock
    RecyclerView mMockToolbox;
    @Mock
    WorkspaceView mMockWorkspaceView;
    @Mock
    Block mDeletableBlock;
    @Mock
    PendingDrag mMockToolboxDrag;
    @Mock
    PendingDrag mMockWorkspaceDrag;

    @Mock
    DragEvent mBlockDragStartFromToolbox;
    @Mock
    DragEvent mBlockDragStartFromWorkspace;
    @Mock
    DragEvent mBlockDragEntered;
    @Mock
    DragEvent mBlockDragLocation;
    @Mock
    DragEvent mBlockDragExited;
    @Mock
    DragEvent mBlockDrop;
    @Mock
    DragEvent mBlockDragEnded;
    @Mock
    DragEvent mRemoteBlockDragEvent;
    @Mock
    DragEvent mOtherDragEvent;

    @Mock
    Block mUnDeletableBlock;
    @Mock
    PendingDrag mUnDeletableBlockDrag;
    @Mock
    DragEvent mUnDeletableBlockDragEvent;

    /** Test instance. */
    OnDragToTrashListener mOnDragToTrashListener;

    ClipDescription mBlockClipDescription = new ClipDescription("block", new String[] {});
    ClipDescription mOtherClipDescription = new ClipDescription("other", new String[] {});

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.stub(mMockController.getClipDataHelper())
                .toReturn(mMockClipDataHelper);

        Mockito.when(mMockToolboxDrag.getDragInitiator()).thenReturn(mMockToolbox);
        Mockito.when(mMockToolboxDrag.getRootDraggedBlock()).thenReturn(mDeletableBlock);
        Mockito.when(mMockWorkspaceDrag.getDragInitiator()).thenReturn(mMockWorkspaceView);
        Mockito.when(mMockWorkspaceDrag.getRootDraggedBlock()).thenReturn(mDeletableBlock);
        Mockito.when(mDeletableBlock.isDeletable()).thenReturn(true);
        Mockito.when(mUnDeletableBlock.isDeletable()).thenReturn(false);

        mockDragEvent(mBlockDragStartFromToolbox,
                DragEvent.ACTION_DRAG_STARTED,
                true,
                mMockToolboxDrag);
        mockDragEvent(mBlockDragStartFromWorkspace,
                DragEvent.ACTION_DRAG_STARTED,
                true,
                mMockWorkspaceDrag);
        mockDragEvent(mBlockDragEntered,
                DragEvent.ACTION_DRAG_ENTERED,
                true,
                mMockWorkspaceDrag);
        mockDragEvent(mBlockDragLocation,
                DragEvent.ACTION_DRAG_LOCATION,
                true,
                mMockWorkspaceDrag);
        mockDragEvent(mBlockDragExited,
                DragEvent.ACTION_DRAG_EXITED,
                true,
                mMockWorkspaceDrag);
        mockDragEvent(mBlockDrop,
                DragEvent.ACTION_DROP,
                true,
                mMockWorkspaceDrag);
        mockDragEvent(mBlockDragEnded,
                DragEvent.ACTION_DRAG_ENDED,
                true,
                null);  // End does not reference the local state.

        mockDragEvent(mRemoteBlockDragEvent, DragEvent.ACTION_DRAG_STARTED, true, null);
        mockDragEvent(mOtherDragEvent, DragEvent.ACTION_DRAG_STARTED, false, null);
        mockDragEvent(mUnDeletableBlockDragEvent, DragEvent.ACTION_DRAG_STARTED, true,
                mUnDeletableBlockDrag);

        mOnDragToTrashListener = new OnDragToTrashListener(mMockController);
    }

    @Test
    public void testIsTrashableBlock() {
        assertThat(mOnDragToTrashListener.isTrashableBlock(mBlockDragStartFromWorkspace)).isTrue();
        assertThat(mOnDragToTrashListener.isTrashableBlock(mBlockDragEntered)).isTrue();
        assertThat(mOnDragToTrashListener.isTrashableBlock(mBlockDragLocation)).isTrue();
        assertThat(mOnDragToTrashListener.isTrashableBlock(mBlockDragExited)).isTrue();
        assertThat(mOnDragToTrashListener.isTrashableBlock(mBlockDrop)).isTrue();

        assertWithMessage("DRAG_ENDED does not have local state (reference to the WorkspaceView)")
                .that(mOnDragToTrashListener.isTrashableBlock(mBlockDragEnded)).isFalse();
        assertWithMessage("Blocks from other activities (no local state) are not trashable.")
                .that(mOnDragToTrashListener.isTrashableBlock(mRemoteBlockDragEvent)).isFalse();
        assertWithMessage("DragEvents that are not recognized blocks are not trashable.")
                .that(mOnDragToTrashListener.isTrashableBlock(mOtherDragEvent)).isFalse();
        assertWithMessage("DragEvents that are not recognized blocks are not trashable.")
                .that(mOnDragToTrashListener.isTrashableBlock(mOtherDragEvent)).isFalse();
    }

    @Test
    public void testOnDrag() {
        Mockito.verify(mMockController).getClipDataHelper();

        assertThat(mOnDragToTrashListener.onDrag(null, mBlockDragStartFromWorkspace)).isTrue();
        Mockito.verifyNoMoreInteractions(mMockController);

        mOnDragToTrashListener.onDrag(null, mBlockDragEntered);
        Mockito.verifyNoMoreInteractions(mMockController);

        mOnDragToTrashListener.onDrag(null, mBlockDragLocation);
        Mockito.verifyNoMoreInteractions(mMockController);

        mOnDragToTrashListener.onDrag(null, mBlockDragExited);
        Mockito.verifyNoMoreInteractions(mMockController);

        assertThat(mOnDragToTrashListener.onDrag(null, mBlockDrop)).isTrue();
        Mockito.verify(mMockController)
                .trashRootBlock(Mockito.any(Block.class));

        mOnDragToTrashListener.onDrag(null, mBlockDragEnded);
        Mockito.verifyNoMoreInteractions(mMockController);
    }

    @Test
    public void testOnDrag_invalid() {
        assertWithMessage("Blocks from other activities (no local state) are not trashable.")
                .that(mOnDragToTrashListener.onDrag(null, mRemoteBlockDragEvent)).isFalse();
        assertWithMessage("DragEvents that are not recognized blocks are not trashable.")
                .that(mOnDragToTrashListener.onDrag(null, mOtherDragEvent)).isFalse();
        assertWithMessage("DragEvents that are not recognized blocks are not trashable.")
                .that(mOnDragToTrashListener.onDrag(null, mUnDeletableBlockDragEvent)).isFalse();
        Mockito.verify(mMockController, Mockito.never())
                .trashRootBlock(Mockito.any(Block.class));
    }

    private void mockDragEvent(
            DragEvent event, int action, boolean isBlock, PendingDrag pending) {
        ClipDescription clipDescrip = isBlock ? mBlockClipDescription : mOtherClipDescription;

        Mockito.when(event.getAction())
                .thenReturn(action);
        Mockito.when(event.getClipDescription())
                .thenReturn(clipDescrip);
        Mockito.when(mMockClipDataHelper.isBlockData(clipDescrip))
                .thenReturn(isBlock);
        Mockito.when(mMockClipDataHelper.getPendingDrag(event))
                .thenReturn(pending);
    }
}

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

package com.google.blockly.android.clipboard;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.view.DragEvent;

import com.google.blockly.android.R;
import com.google.blockly.android.ui.PendingDrag;
import com.google.blockly.model.Block;
import com.google.blockly.model.IOOptions;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.IOException;

/**
 * Implements ClipDataTransformer with a single supported MIME type.  Uses intent extras as the
 * in-transit storage format.
 */
public class SingleMimeTypeClipDataHelper implements BlockClipDataHelper {
    public static final String EXTRA_BLOCKLY_XML = "BLOCKLY_XML";

    /**
     * This constructs a {@link SingleMimeTypeClipDataHelper} with a MIME type derived
     * from the application's package name. This assumes all Blockly workspaces in an app use with
     * a shared set of blocks, and blocks can be dragged/copied/pasted them, even if they are
     * in different Activities. It also ensures blocks from other applications will be rejected.
     *
     * @param context
     * @return
     */
    public static BlockClipDataHelper getDefault(Context context) {
        String mimeType = "application/x-blockly-" + context.getPackageName() + "+xml";

        // TODO(#): Singular vs plural ("block" vs "blocks")
        String label = context.getResources().getString(R.string.blockly_clipdata_label_default);

        return new SingleMimeTypeClipDataHelper(mimeType, label);
    }

    protected final String mMimeType;
    protected final String mClipLabel;

    /**
     * Constructs a new {@link SingleMimeTypeClipDataHelper} with the provided MIME string and
     * user visible (accessibility, etc.) clip label string.
     *
     * @param mimeType The MIME type the new instance use for encoding and decoding.
     * @param clipLabel The human readable label to apply to {@link ClipData}s.
     */
    public SingleMimeTypeClipDataHelper(String mimeType, String clipLabel) {
        mMimeType = mimeType;
        mClipLabel = clipLabel;
    }

    @Override
    public ClipData buildDragClipData(PendingDrag drag) throws IOException {
        Block root = drag.getRootDraggedBlock();
        String xml = BlocklyXmlHelper.writeBlockToXml(root, IOOptions.WRITE_ALL_DATA);

        Intent intent = new Intent();
        intent.putExtra(EXTRA_BLOCKLY_XML, xml);

        // TODO(#489): Encode shadow size/offset/zoom info for remote drop targets.
        ClipData.Item item = new ClipData.Item(intent);

        return new ClipData(mClipLabel, new String[] {mMimeType}, item);
    }

    /**
     * @param descript A description of the incoming clipboard data.
     * @return True if the MIME type is found.
     */
    @Override
    public boolean isBlockData(ClipDescription descript) {
        String[] mimeTypes =(descript == null) ? null : descript.filterMimeTypes(mMimeType);
        return mimeTypes != null && mimeTypes.length > 0;
    }

    /**
     * @param event The DragEvent containing the PendingDrag.
     * @return The PendingDrag containing the dragged blocks.
     */
    @Override
    public PendingDrag getPendingDrag(DragEvent event) {
        if (!isBlockData(event.getClipDescription())) {
            return null;
        }
        // In the future, this will support drags across application boundaries, constructing a new
        // PendingDrag as necessary. For now, it just extracts the PendingData from the local state.
        return (PendingDrag) event.getLocalState();
    }
}

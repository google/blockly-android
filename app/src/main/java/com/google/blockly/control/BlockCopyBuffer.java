/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.control;

import android.support.annotation.Nullable;

import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Holds serialized BlockGroups to handle copy-paste operations within and between workspaces.
 */
public class BlockCopyBuffer {
    private static final BlocklyXmlHelper mXmlHelper = new BlocklyXmlHelper();
    private String mXmlString;

    /**
     * Serializes toCopy, including all of its inputs and all blocks that follow it (i.e. follows
     * the chain of nextConnections until it finds a null block.  Saves the result in mXmlString.
     *
     * @param toCopy The block to copy into the buffer.
     */
    public void setBufferContents(List<Block> toCopy) throws BlocklySerializerException {
        mXmlString = "";
        if (toCopy == null) {
            return;
        }

        StringOutputStream os = new StringOutputStream();
        mXmlHelper.writeToXml(toCopy, os);
        mXmlString = os.toString();
    }

    /**
     * Loads from XML the group of blocks stored in mXmlString, which must have been put there by a
     * call to setBufferContents.
     *
     * @param blockFactory The BlockFactory for the workspace where the blocks are being loaded.
     * @return A Block or chain of Blocks, or null if there were no previous successful calls to
     * setBufferContents.
     */
    @Nullable
    public List<Block> getBufferContents(BlockFactory blockFactory) {
        if (mXmlString.isEmpty()) {
            return null;
        }

        return mXmlHelper.loadFromXml(
                new ByteArrayInputStream(mXmlString.getBytes()), blockFactory, null);
    }
}

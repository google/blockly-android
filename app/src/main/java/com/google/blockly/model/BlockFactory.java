/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class for building a set of master blocks and then obtaining copies of them for use in
 * a workspace or toolbar.
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    private Resources mResources;
    private HashMap<String, Block> mBlockTemplates = new HashMap<>();

    /**
     * Create a factory with an initial set of blocks from json resources.
     *
     * @param context The context for loading resources.
     * @param blockSourceIds A list of JSON resources containing blocks.
     */
    public BlockFactory(Context context, int[] blockSourceIds) {
        mResources = context.getResources();
        if (blockSourceIds != null) {
            for (int i = 0; i < blockSourceIds.length; i++) {
                loadBlocksFromResource(blockSourceIds[i]);
            }
        }
    }

    /**
     * Adds a block to the set of blocks that can be created.
     *
     * @param block The master block to add.
     */
    public void addBlockTemplate(Block block) {
        if (mBlockTemplates.containsKey(block.getName())) {
            throw new IllegalArgumentException("There is already a block named " + block.getName());
        }
        mBlockTemplates.put(block.getName(), new Block.Builder(block).build());
    }

    /**
     * Removes a block type from the factory.
     *
     * @param prototypeName The name of the block to remove.
     * @return The master block that was removed or null if it wasn't found.
     */
    public Block removeBlockTemplate(String prototypeName) {
        return mBlockTemplates.remove(prototypeName);
    }

    /**
     * Creates a block of the specified type using one of the master blocks known to this factory.
     * If the prototypeName is not one of the known block types null will be returned instead.
     *
     * @param prototypeName The name of the block type to create.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     * @return A new block of that type or null.
     */
    public Block obtainBlock(String prototypeName, String uuid) {
        if (!mBlockTemplates.containsKey(prototypeName)) {
            Log.w(TAG, "Block " + prototypeName + " not found.");
            return null;
        }
        Block.Builder bob = new Block.Builder(mBlockTemplates.get(prototypeName));
        if (uuid != null) {
            bob.setUuid(uuid);
        }
        return bob.build();
    }

    /**
     * @return The list of known blocks that can be created.
     */
    public List<Block> getAllBlocks() {
        return new ArrayList<Block>(mBlockTemplates.values());
    }

    /**
     * Adds a set of master blocks from a JSON resource.
     *
     * @param resId The id of the JSON resource to load blocks from.
     */
    public void loadBlocksFromResource(int resId) {
        InputStream blockIs = mResources.openRawResource(resId);
        loadBlocks(blockIs);
    }

    private void loadBlocks(InputStream blockIs) {
        try {
            int size = blockIs.available();
            byte[] buffer = new byte[size];
            blockIs.read(buffer);

            String json = new String(buffer, "UTF-8");
            JSONArray blocks = new JSONArray(json);
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                String id = block.optString("id");
                if (!TextUtils.isEmpty(id)) {
                    mBlockTemplates.put(id, Block.fromJson(id, block));
                } else {
                    throw new IllegalArgumentException("Block " + i
                            + " has no id and cannot be loaded.");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading stream.", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON.", e);
        }
    }
}

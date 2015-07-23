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

package com.google.blockly.blocks;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by epastern on 5/21/15.
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    private Resources mResources;
    private HashMap<String, Block> mBlockTemplates = new HashMap<>();

    public BlockFactory(Context context, int[] blockSourceIds) {
        mResources = context.getResources();
        if (blockSourceIds != null) {
            for (int i = 0; i < blockSourceIds.length; i++) {
                loadBlocksFromResource(blockSourceIds[i]);
            }
        }
    }

    public void addBlockTemplate(Block block) {
        if (mBlockTemplates.containsKey(block.getName())) {
            throw new IllegalArgumentException("There is already a block named " + block.getName());
        }
        mBlockTemplates.put(block.getName(), new Block.Builder(block).build());
    }

    public Block obtainBlock(String prototypeName) {
        if (!mBlockTemplates.containsKey(prototypeName)) {
            Log.w(TAG, "Block " + prototypeName + " not found.");
            return null;
        }
        Block.Builder bob = new Block.Builder(mBlockTemplates.get(prototypeName));
        return bob.build();
    }

    public List<Block> getAllBlocks() {
        return new ArrayList<Block>(mBlockTemplates.values());
    }

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

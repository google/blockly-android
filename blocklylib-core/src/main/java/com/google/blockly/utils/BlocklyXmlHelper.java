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

package com.google.blockly.utils;

import android.support.annotation.Nullable;

import com.google.blockly.android.control.WorkspaceStats;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklyParserException;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.ToolboxCategory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to serialize and deserialize blockly workspaces, including constructing new
 * parsers and serializers as needed.
 */
public final class BlocklyXmlHelper {
    private static final String XML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static final XmlPullParserFactory mParserFactory = createParseFactory();

    private BlocklyXmlHelper() {
    }

    /**
     * Loads toolbox from XML.  Each category may have multiple subcategories and/or multiple blocks
     * contained in it or descending from it.
     *
     * @param is The input stream from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     *
     * @return The top-level category in the toolbox.
     * @throws BlocklyParserException when parsing fails.
     */
    public static ToolboxCategory loadToolboxFromXml(InputStream is, BlockFactory blockFactory)
            throws BlocklyParserException {
        try {
            XmlPullParser parser = mParserFactory.newPullParser();
            parser.setInput(is, null);
            return ToolboxCategory.fromXml(parser, blockFactory);
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklyParserException(e);
        }
    }

    /**
     * Loads a list of top-level Blocks from XML.  Each top-level Block may have many Blocks
     * contained in it or descending from it.
     *
     * @param is The input stream from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     * @param stats The WorkspaceStats to store connection information in.
     * @param result The List to add the parsed blocks to.
     *
     * @throws BlocklyParserException
     */
    public static void loadFromXml(InputStream is, BlockFactory blockFactory, WorkspaceStats stats,
            List<Block> result) throws BlocklyParserException {
        loadBlocksFromXml(is, blockFactory, stats, result);
    }

    /**
     * Convenience function that creates a new {@link ArrayList}.
     */
    public static List<Block> loadFromXml(InputStream is, BlockFactory blockFactory,
            WorkspaceStats stats) throws BlocklyParserException {
        List<Block> result = new ArrayList<>();
        loadBlocksFromXml(is, blockFactory, stats, result);
        return result;
    }

    /**
     * Convenience function to load only one Block.
     *
     * @param is The input stream from which to read the Block.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     *
     * @return The first Block read from is, or null if no Block was read.
     * @throws BlocklyParserException
     */
    @Nullable
    public static Block loadOneBlockFromXml(InputStream is, BlockFactory blockFactory)
            throws BlocklyParserException {
        List<Block> temp = loadFromXml(is, blockFactory, null);
        if (temp == null || temp.isEmpty()) {
            return null;
        }
        return temp.get(0);
    }

    /**
     * Serializes all Blocks in the given list and writes them to the given output stream.
     *
     * @param toSerialize A list of Blocks to serialize.
     * @param os An OutputStream to which to write them.
     *
     * @throws BlocklySerializerException
     */
    public static void writeToXml(List<Block> toSerialize, OutputStream os)
            throws BlocklySerializerException {
        try {
            XmlSerializer serializer = mParserFactory.newSerializer();
            serializer.setOutput(os, null);
            serializer.setPrefix("", XML_NAMESPACE);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(XML_NAMESPACE, "xml");
            for (int i = 0; i < toSerialize.size(); i++) {
                toSerialize.get(i).serialize(serializer, true);
            }
            serializer.endTag(XML_NAMESPACE, "xml");
            serializer.flush();
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklySerializerException(e);
        }
    }

    /**
     * Convenience function to serialize only one Block.
     *
     * @param toSerialize A Block to serialize.
     * @param os An OutputStream to which to write them.
     *
     * @throws BlocklySerializerException
     */
    public static void writeOneBlockToXml(Block toSerialize, OutputStream os)
            throws BlocklySerializerException {
        List<Block> temp = new ArrayList<>();
        temp.add(toSerialize);
        writeToXml(temp, os);
    }

    /**
     * Loads a list of top-level Blocks from XML.  Each top-level Block may have many Blocks
     * contained in it or descending from it.
     *
     * @param is The input stream from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     *
     * @return A list of top-level Blocks.
     * @throws BlocklyParserException
     */
    private static void loadBlocksFromXml(
            InputStream is, BlockFactory blockFactory, WorkspaceStats stats, List<Block> result)
            throws BlocklyParserException {
        try {
            XmlPullParser parser = mParserFactory.newPullParser();
            parser.setInput(is, null);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName() == null) {
                            throw new BlocklyParserException("Malformed XML; aborting.");
                        }
                        if (parser.getName().equalsIgnoreCase("block")) {
                            result.add(Block.fromXml(parser, blockFactory));
                        } else if (parser.getName().equalsIgnoreCase("shadow")) {
                            throw new IllegalArgumentException(
                                    "Shadow blocks may not be top level blocks.");
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklyParserException(e);
        }
    }

    private static XmlPullParserFactory createParseFactory() throws BlocklyParserException {
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new BlocklyParserException(e);
        }
        parserFactory.setNamespaceAware(true);
        return parserFactory;
    }
}

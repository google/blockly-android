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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.blockly.android.control.WorkspaceStats;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklyCategory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.IOOptions;
import com.google.blockly.model.Mutator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to serialize and deserialize blockly workspaces, including constructing new
 * parsers and serializers as needed.
 */
public final class BlocklyXmlHelper {
    private static final String XML_NAMESPACE = "http://www.w3.org/1999/xhtml";
    private static final XmlPullParserFactory PARSER_FACTORY = createParseFactory();


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
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    public static BlocklyCategory loadToolboxFromXml(InputStream is, BlockFactory blockFactory)
            throws BlockLoadingException {
        try {
            XmlPullParser parser = PARSER_FACTORY.newPullParser();
            parser.setInput(is, null);
            return BlocklyCategory.fromXml(parser, blockFactory);
        } catch (XmlPullParserException e) {
            throw new BlockLoadingException(e);
        }
    }

    /**
     * Loads a list of top-level Blocks from XML.  Each top-level Block may have many Blocks
     * contained in it or descending from it.
     *
     * @param inputXml The input stream from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     * @param result The List to add the parsed blocks to.
     *
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    public static void loadFromXml(InputStream inputXml, BlockFactory blockFactory,
                                   List<Block> result)
            throws BlockLoadingException {
        loadBlocksFromXml(inputXml, null, blockFactory, result);
    }

    /**
     * Loads a list of top-level Blocks from XML.  Each top-level Block may have many Blocks
     * contained in it or descending from it.
     *
     * @param inputXml The input stream of XML from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     * @param stats Unused
     * @param result The List to add the parsed blocks to.
     *
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    @Deprecated
    public static void loadFromXml(InputStream inputXml, BlockFactory blockFactory,
                                   WorkspaceStats stats, List<Block> result)
            throws BlockLoadingException {
        loadBlocksFromXml(inputXml, null, blockFactory, result);
    }

    /**
     * Convenience function that creates a new {@link ArrayList}.
     * @param inputXml The input stream of XML from which to read.
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    public static List<Block> loadFromXml(InputStream inputXml, BlockFactory blockFactory)
            throws BlockLoadingException {
        List<Block> result = new ArrayList<>();
        loadBlocksFromXml(inputXml, null, blockFactory, result);
        return result;
    }

    /**
     * Convenient version of {@link #loadFromXml(InputStream, BlockFactory, List)} function that
     * returns results in a newly created a new {@link ArrayList}.
     * @param inputXml The input stream of XML from which to read.
     * @param blockFactory The factory object used to create blocks.
     * @param stats Unused.
     * @return A list of the root blocks successfully loaded.
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    @Deprecated
    public static List<Block> loadFromXml(InputStream inputXml, BlockFactory blockFactory,
                                          WorkspaceStats stats) throws BlockLoadingException {
        return loadFromXml(inputXml, blockFactory);
    }

    /**
     * Convenience function to load only one Block.
     *
     * @param inputXml The input stream of XML from which to read.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     *
     * @return The first Block read from is, or null if no Block was read.
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    @Nullable
    public static Block loadOneBlockFromXml(InputStream inputXml, BlockFactory blockFactory)
            throws BlockLoadingException {
        List<Block> result = new ArrayList<>();
        loadBlocksFromXml(inputXml, null, blockFactory, result);
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            throw new IllegalStateException("Expected one top block. Found " + result.size() + ".");
        }
        return result.get(0);
    }

    /**
     * Convenience function to load only one Block.
     *
     * @param xml The XML in string form to read the block from.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     *
     * @return The first Block read from is, or null if no Block was read.
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    @Nullable
    public static Block loadOneBlockFromXml(String xml, BlockFactory blockFactory)
            throws BlockLoadingException {
        List<Block> result = new ArrayList<>();
        loadBlocksFromXml(null, xml, blockFactory, result);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Serializes all Blocks in the given list and writes them to the given output stream.
     *
     * @param toSerialize A list of Blocks to serialize.
     * @param os An OutputStream to write the blocks to.
     * @param options The options to configure the block serialization. If omitted,
     *                {@link IOOptions#WRITE_ALL_DATA} will be used by default.
     *
     * @throws BlocklySerializerException
     */
    public static void writeToXml(@NonNull List<Block> toSerialize, @NonNull OutputStream os,
                                  @Nullable IOOptions options)
            throws BlocklySerializerException {
        writeToXmlImpl(toSerialize, os, null, options);
    }

    /**
     * Serializes all Blocks in the given list and writes them to the given writer.
     *
     * @param toSerialize A list of Blocks to serialize.
     * @param writer A writer to write the blocks to.
     * @param options The options to configure the block serialization. If omitted,
     *                {@link IOOptions#WRITE_ALL_DATA} will be used by default.
     *
     * @throws BlocklySerializerException
     */
    public static void writeToXml(@NonNull List<Block> toSerialize, @NonNull Writer writer,
                                  @Nullable IOOptions options)
            throws BlocklySerializerException {
        writeToXmlImpl(toSerialize, null, writer, options);
    }

    /**
     * Serializes all Blocks in the given list and writes them to the either the output stream or
     * writer, whichever is not null.
     *
     * @param toSerialize A list of Blocks to serialize.
     * @param os An OutputStream to write the blocks to.
     * @param writer A writer to write the blocks to, if {@code os} is null.
     * @param options The options to configure the block serialization. If omitted,
     *                {@link IOOptions#WRITE_ALL_DATA} will be used by default.
     *
     * @throws BlocklySerializerException
     */
    public static void writeToXmlImpl(@NonNull List<Block> toSerialize, @Nullable OutputStream os,
                                      @Nullable Writer writer, @Nullable IOOptions options)
            throws BlocklySerializerException {
        if (options == null) {
            options = IOOptions.WRITE_ALL_DATA;
        }
        try {
            XmlSerializer serializer = PARSER_FACTORY.newSerializer();
            if (os != null) {
                serializer.setOutput(os, null);
            } else {
                serializer.setOutput(writer);
            }
            serializer.setPrefix("", XML_NAMESPACE);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(XML_NAMESPACE, "xml");
            for (int i = 0; i < toSerialize.size(); i++) {
                toSerialize.get(i).serialize(serializer, true, options);
            }
            serializer.endTag(XML_NAMESPACE, "xml");
            serializer.flush();
        } catch (XmlPullParserException | IOException e) {
            throw new BlocklySerializerException(e);
        }
    }

    /**
     * Convenience function to serialize one stack of Blocks (a BlockGroup, effectively).
     *
     * @param rootBlock The root block of the stack to serialize.
     * @param os An OutputStream to which to write them.
     * @param options The options to configure the block serialization. If omitted,
     *                {@link IOOptions#WRITE_ALL_DATA} will be used by default.
     *
     * @throws BlocklySerializerException
     */
    public static void writeBlockToXml(@NonNull Block rootBlock, @NonNull OutputStream os,
                                       @Nullable IOOptions options)
            throws BlocklySerializerException {
        List<Block> temp = new ArrayList<>();
        temp.add(rootBlock);
        writeToXml(temp, os, options);
    }

    /**
     * Convenience function to serialize one stack of Blocks (a BlockGroup, effectively).
     *
     * @param rootBlock The root block of the stack to serialize.
     * @param options The options to configure the block serialization. If omitted,
     *                {@link IOOptions#WRITE_ALL_DATA} will be used by default.
     * @return XML string for block and all descendant blocks.
     * @throws BlocklySerializerException
     */
    public static String writeBlockToXml(@NonNull Block rootBlock, @Nullable IOOptions options)
            throws BlocklySerializerException {
        StringWriter sw = new StringWriter();
        List<Block> temp = new ArrayList<>();
        temp.add(rootBlock);
        writeToXml(temp, sw, options);
        String xmlString = sw.toString();
        try {
            sw.close();
            return xmlString;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param text The text to escape.
     * @return
     */
    public static String escape(String text) {
        try {
            StringWriter sw = new StringWriter();
            XmlSerializer serializer = PARSER_FACTORY.newSerializer();
            serializer.setOutput(sw);
            serializer.text(text).flush();
            return sw.toString();
        } catch(XmlPullParserException | IOException e) {
            // Should never get here.
            throw new IllegalStateException("Unable to build/use XmlSerializer.");
        }
    }

    /**
     * Loads a list of top-level Blocks from XML.  Each top-level Block may have many Blocks
     * contained in it or descending from it.
     *
     * @param inStream The input stream to read blocks from. Maybe null.
     * @param inString The xml string to read blocks from if {@code insStream} is null.
     * @param blockFactory The BlockFactory for the workspace where the Blocks are being loaded.
     * @param result An list (usually empty) to append new top-level Blocks to.
     *
     * @throws BlockLoadingException If any error occurs with the input. It may wrap an IOException
     *                               or XmlPullParserException as a root cause.
     */
    private static void loadBlocksFromXml(
            InputStream inStream, String inString, BlockFactory blockFactory, List<Block> result)
            throws BlockLoadingException {
        StringReader reader = null;
        try {
            XmlPullParser parser = PARSER_FACTORY.newPullParser();
            if (inStream != null) {
                parser.setInput(inStream, null);
            } else {
                reader = new StringReader(inString);
                parser.setInput(reader);
            }
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName() == null) {
                            throw new BlockLoadingException("Malformed XML; aborting.");
                        }
                        if (parser.getName().equalsIgnoreCase("block")) {
                            result.add(blockFactory.fromXml(parser));
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
            throw new BlockLoadingException(e);
        }
        if (reader != null) {
            reader.close();
        }
    }

    /**
     * Serializes the current element and all child nodes as a String.
     * @param parser The parser to pull from.
     * @return The composed element string.
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static String captureElement(XmlPullParser parser)
            throws XmlPullParserException, IOException
    {
        int event = parser.getEventType();
        if (event != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("Expected call to begin at START_TAG");
        }
        int depth = 0;

        StringWriter sw = new StringWriter();
        XmlSerializer serializer = PARSER_FACTORY.newSerializer();
        serializer.setOutput(sw);

        String namespace, prefix;
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    ++depth;
                    namespace = parser.getNamespace();
                    prefix = parser.getPrefix();
                    if (namespace != null && prefix != null) {
                        serializer.setPrefix(prefix, namespace);
                    }
                    serializer.startTag(namespace, parser.getName());
                    int attrCount = parser.getAttributeCount();
                    for (int i = 0; i < attrCount; ++i) {
                        namespace = parser.getAttributeNamespace(i);
                        prefix = parser.getAttributePrefix(i);
                        if (namespace != null && prefix != null) {
                            serializer.setPrefix(prefix, namespace);
                        }
                        serializer.attribute(namespace, parser.getAttributeName(i),
                                parser.getAttributeValue(i));
                    }
                    break;

                case XmlPullParser.TEXT:
                case XmlPullParser.IGNORABLE_WHITESPACE:
                    serializer.text(parser.getText());
                    break;

                case XmlPullParser.CDSECT:
                    serializer.cdsect(parser.getText());
                    break;

                case XmlPullParser.END_TAG:
                    namespace = parser.getNamespace();
                    if (namespace != null) {
                        serializer.setPrefix(parser.getPrefix(), namespace);
                    }
                    serializer.endTag(namespace, parser.getName());
                    --depth;
                    break;
            }
            if (depth <= 0) {
                serializer.flush();
                return sw.toString();
            }

            event = parser.next();
        }
        throw new IOException("Unexpected end of document.");
    }

    /**
     * Performs the XML IO boilerplate used to update {@code mutator} with the {@code <mutation>}
     * element provided in {@code mutation}.
     * @param block The block containing the Mutator.
     * @param mutator The Mutator to be updated.
     * @param mutation The {@code <mutation>} element as a string.
     * @throws BlockLoadingException
     */
    public static void updateMutator(
            @NonNull Block block, @NonNull Mutator mutator, @NonNull String mutation)
            throws BlockLoadingException
    {
        try {
            Reader reader = new StringReader(mutation);
            XmlPullParser parser = PARSER_FACTORY.newPullParser();
            parser.setInput(reader);
            mutator.update(block, parser);
        } catch (XmlPullParserException | IOException e) {
            throw new BlockLoadingException("Failed to parse mutation: " + mutation, e);
        }
    }

    private static XmlPullParserFactory createParseFactory() {
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new IllegalStateException(e);
        }
        parserFactory.setNamespaceAware(true);
        return parserFactory;
    }
}

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

import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklyParserException;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Holds serialized BlockGroups to handle copy-paste operations within and between workspaces.
 */
public class BlockCopyBuffer {
    private final BlockFactory mBlockFactory;
    private String mXmlString;

    public BlockCopyBuffer(BlockFactory blockFactory) {
        mBlockFactory = blockFactory;
    }

    /**
     * Serializes toCopy, including all of its inputs and all blocks that follow it (i.e. follows
     * the chain of nextConnections until it finds a null block.  Saves the result in mXmlString.
     *
     * @param toCopy The block to copy into the buffer.
     */
    public void setBufferContents(Block toCopy) throws BlocklySerializerException {
        mXmlString = "";
        if (toCopy == null) {
            return;
        }

        StringWriter writer = new StringWriter();
        try {
            XmlPullParserFactory xppfactory = XmlPullParserFactory.newInstance();
            xppfactory.setNamespaceAware(true);

            XmlSerializer serializer = xppfactory.newSerializer();
            serializer.setOutput(writer);
            serializer.setPrefix("", Workspace.XML_NAMESPACE);

            // Even if we serialize multiple blocks, there has to be one top level xml tag.
            // That can be the <xml> </xml> tag.
            serializer.startTag(Workspace.XML_NAMESPACE, "xml");
            toCopy.serialize(serializer, true /* rootBlock */);

            serializer.endTag(Workspace.XML_NAMESPACE, "xml");
            serializer.flush();
            mXmlString = writer.toString();
        } catch (XmlPullParserException e) {
            mXmlString = "";
            throw new BlocklySerializerException(e);
        } catch (IOException e) {
            mXmlString = "";
            throw new BlocklySerializerException(e);
        }
    }

    public Block getBufferContents() {
        if (mXmlString.isEmpty())
            return null;

        try {
            XmlPullParserFactory Xppfactory = XmlPullParserFactory.newInstance();
            Xppfactory.setNamespaceAware(true);
            XmlPullParser parser = Xppfactory.newPullParser();

            parser.setInput(new ByteArrayInputStream(mXmlString.getBytes()), null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (parser.getName() == null) {
                            throw new BlocklyParserException("Malformed XML; aborting.");
                        }
                        if (parser.getName().equalsIgnoreCase("block")) {
                            return Block.fromXml(parser, mBlockFactory);
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            throw new BlocklyParserException(e);
        } catch (IOException e) {
            throw new BlocklyParserException(e);
        }
        return null;
    }
}

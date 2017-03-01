/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Interface for mutators, platform specific hooks into blocks that manage the {@code <mutation>}
 * serialized state and any related block updates.
 */
public interface Mutator {
    /**
     * Serializes the Mutator's state to an XML {@code <mutation>} element.
     *
     * Compare with {@code block.domToMutation()} on web Blockly (added by extensions or mixins),
     * or {@code Mutator.toXMLElement()} on blockly-ios.
     *
     * @param serializer
     * @throws IOException
     */
    void serialize(XmlSerializer serializer) throws IOException;

    /**
     * Updates the mutator state from the provided {@code <mutation>} XML.
     * @param block The block with the mutator.
     * @param parser The parser with the {@code <mutation>} element.
     * @throws IOException
     * @throws XmlPullParserException
     */
    void update(Block block, XmlPullParser parser) throws IOException, XmlPullParserException;
}

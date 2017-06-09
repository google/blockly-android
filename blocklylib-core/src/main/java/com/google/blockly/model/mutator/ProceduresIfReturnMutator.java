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
package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * This mutator supports the {@code procedures_ifreturn} block, configuring the presence of the
 * return value input and the block's disabled state.
 */
public class ProceduresIfReturnMutator extends Mutator {
    public static final String MUTATOR_ID = "procedures_ifreturn_mutator";
    public static final Mutator.Factory<ProceduresIfReturnMutator> FACTORY =
            new Mutator.Factory<ProceduresIfReturnMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public ProceduresIfReturnMutator newMutator(BlocklyController controller) {
                    return new ProceduresIfReturnMutator(this);
                }
            };

    /**
     * Constructs a new {@code procedures_ifreturn_mutator} mutator.
     * @param factory The factory the constructed this mutator.
     */
    public ProceduresIfReturnMutator(Factory<ProceduresIfReturnMutator> factory) {
        super(factory);
    }

    @Override
    protected void onAttached(Block block) {
        super.onAttached(block);

        // TODO: Find parent definition. Set enabled/disabled state. Update return value input.
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // Do nothing.  No state.
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        // Do nothing.  No state.
    }
}

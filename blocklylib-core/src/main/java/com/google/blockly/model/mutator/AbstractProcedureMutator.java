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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.ProcedureManager;
import com.google.blockly.model.Block;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.model.ProcedureInfo;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all procedure definition and procedure call mutators, providing a base
 * implementation of the mutation state variables and related I/O.
 */
public abstract class AbstractProcedureMutator<Info extends ProcedureInfo> extends Mutator {
    private static final String TAG = "AbstractProcedureMutator";

    protected final BlocklyController mController;
    protected final ProcedureManager mProcedureManager;
    protected Info mProcedureInfo = null;

    public final ProcedureInfo getProcedureInfo() {
        return mProcedureInfo;
    }

    public abstract void mutate(ProcedureInfo info);

    /**
     * Base constructor for subclasses.
     * @param factory The factory constructing this mutator.
     * @param controller The {@link BlocklyController} for this activity.
     */
    protected AbstractProcedureMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
        mController = controller;
        mProcedureManager = mController.getWorkspace().getProcedureManager();
    }

    /**
     * Sets the procedure name for this mutator (and thus {@code mBlock}) when it is not on the
     * workspace, not managed by the ProcedureManager.
     * @param newProcedureName The new name to use.
     */
    final public void setProcedureName(@NonNull String newProcedureName) {
        String oldName = getProcedureName();
        if (mProcedureManager.getDefinitionBlocks().get(oldName) == mBlock) {
            throw new IllegalStateException(
                    "Rename procedure managed by the ProcedureManager "
                            + "using ProcedureManager.mutateProcedure(..).");
        }
        setProcedureNameImpl(newProcedureName);
    }

    /**
     * @return The procedure name associated with this mutator. May be null if not attached to a
     *         block.
     */
    @Nullable
    public String getProcedureName() {
        return (mProcedureInfo == null) ? null : mProcedureInfo.getProcedureName();
    }

    /**
     * @return The list of argument names.
     */
    @NonNull
    public final List<String> getArgumentNameList() {
        return mProcedureInfo == null ?
                Collections.<String>emptyList()
                : mProcedureInfo.getArgumentNames();
    }

    /**
     * @param serializer The {@link XmlSerializer} to output to.
     * @throws IOException If the stream backing {@code serializer} fails.
     */
    @Override
    public final void serialize(XmlSerializer serializer) throws IOException {
        serializeInfo(serializer, mProcedureInfo);
    }

    /**
     * Updates the block using the mutation in the XML.
     *
     * @param parser The parser with the {@code <mutation>} element.
     * @throws IOException If the input stream fails.
     * @throws XmlPullParserException If the input is not valid XML.
     * @throws BlockLoadingException If the input is not a valid procedure mutation.
     */
    @Override
    public void update(final XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        mProcedureInfo = parseAndValidateMutationXml(parser);
        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                updateBlock();  // May fire events if block fields are updated (NAME, in particular)
            }
        });
    }

    /**
     * Updates the ProcedureInfo with a new name, and updates the name field. This should never be
     * called directly. Use {@link #setProcedureName(String)} or {@link #mutate(ProcedureInfo)}.
     * @param newProcedureName The updated name. Cannot be null.
     */
    protected abstract void setProcedureNameImpl(@NonNull String newProcedureName);

    /**
     * Writes an XML mutation string for the provided values.
     *
     * @param procedureInfo The procedure info to write.
     * @return Serialized XML {@code <mutation>} tag, encoding the values.
     */
    protected String writeMutationString(final Info procedureInfo) {
        try {
            return BlocklyXmlHelper.writeXml(new BlocklyXmlHelper.XmlContentWriter() {
                @Override
                public void write(XmlSerializer serializer) throws IOException {
                    serializeInfo(serializer, procedureInfo);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write mutation string.", e);
        }
    }

    @Override
    protected void onAttached(Block block) {
        updateBlock();
    }

    // Input/output methods are protected because the creation of differs.
    // Could become an extension point for supporting other procedure/mutators types
    protected abstract void serializeInfo(XmlSerializer serializer, Info info)
            throws IOException;
    protected abstract Info parseAndValidateMutationXml(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException;

    /**
     * Applies the mutation to {@code mBlock}.
     */
    protected void updateBlock() {
        if (mProcedureInfo != null) {
            mBlock.reshape(buildUpdatedInputs());
        }
    }

    /**
     * @return A new list of {@link Input Inputs} with which to {@link Block#reshape reshape} the
     *         block during mutation.
     */
    protected abstract List<Input> buildUpdatedInputs();
}

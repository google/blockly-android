package com.google.blockly.model.mutator;

import android.support.annotation.VisibleForTesting;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklyEvent;
import com.google.blockly.model.Field;
import com.google.blockly.model.FieldDropdown;
import com.google.blockly.model.Input;
import com.google.blockly.model.Mutator;
import com.google.blockly.utils.BlockLoadingException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements <code>math_is_divisibleby_mutator</code> {@link Mutator} for the
 * <code>math_number_property</code> block, which adds the {@code DIVISOR} input if the property is
 * "is divisible by". This mutator has no serialized state.
 */
public final class MathIsDivisibleByMutator extends Mutator {
    private static final String[] NUMBER_CHECK = {"Number"};
    private static final String DIVISIBLE_BY = "DIVISIBLE_BY";

    public static final String MUTATOR_ID = "math_is_divisibleby_mutator";

    public static final Mutator.Factory<MathIsDivisibleByMutator> FACTORY =
            new Mutator.Factory<MathIsDivisibleByMutator>() {
                @Override
                public String getMutatorId() {
                    return MUTATOR_ID;
                }

                @Override
                public MathIsDivisibleByMutator newMutator(BlocklyController controller) {
                    return new MathIsDivisibleByMutator(this, controller);
                }
            };

    private Block mBlock;
    private BlocklyController mController;
    @VisibleForTesting FieldDropdown mDropdown;

    private Input mDivisorInput =
            new Input.InputValue("DIVISOR", /* fields */ null, /* alignment */ null, NUMBER_CHECK);

    private List<Input> mInputsWithoutDivisor;
    private List<Input> mInputsWithDivisor;

    protected MathIsDivisibleByMutator(Mutator.Factory factory, BlocklyController controller) {
        super(factory);
        this.mController = controller;
    }

    @Override
    public void onAttached(Block block) {
        if (mBlock != null) {
            throw new IllegalStateException("Cannot reuse mutator.");
        }
        Field propertyField = block.getFieldByName("PROPERTY");
        if (propertyField == null || !(propertyField instanceof FieldDropdown)) {
            throw new IllegalStateException("FieldDropDown \"PROPERTY\" not found.");
        }

        mBlock = block;
        mDropdown = (FieldDropdown) propertyField;

        mInputsWithoutDivisor = mBlock.getInputs();
        mInputsWithDivisor = new ArrayList<>(mInputsWithoutDivisor.size() + 1);
        mInputsWithDivisor.addAll(mInputsWithoutDivisor);
        mInputsWithDivisor.add(mDivisorInput);

        mBlock.setEventCallback(new BlocklyController.EventsCallback() {
            @Override
            public int getTypesBitmask() {
                return BlocklyEvent.TYPE_CHANGE;
            }

            @Override
            public void onEventGroup(List<BlocklyEvent> events) {
                updateShape();
            }
        });
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        // Do nothing. No serialized state.
    }

    @Override
    public void update(XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // Do nothing. No serialized state.
    }

    private void updateShape() {
        mController.groupAndFireEvents(new Runnable() {
            @Override
            public void run() {
                boolean isSelected = mDropdown.getSelectedValue().equals(DIVISIBLE_BY);
                boolean isShown = mDivisorInput.getBlock() != null;
                if (isSelected != isShown) {
                    if (isShown) {
                        Block connectedBlock = mDivisorInput.getConnectedBlock();
                        if (connectedBlock != null) {
                            mController.extractBlockAsRoot(connectedBlock);
                        }

                    }
                    mBlock.reshape(isSelected ? mInputsWithDivisor : mInputsWithoutDivisor);
                }
            }
        });
    }
}

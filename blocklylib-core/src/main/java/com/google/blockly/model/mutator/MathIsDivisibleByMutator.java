package com.google.blockly.model.mutator;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlocklyEvent;
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
 * <code>math_number_property</code> block. This mutator has no serialized state.
 */
public final class MathIsDivisibleByMutator extends Mutator {
    private static final String[] NUMBER_CHECK = {"Number"};
    private static final String DIVISIBLE_BY = "DIVISIBLE_BY";

    public static final class Factory implements Mutator.Factory<MathIsDivisibleByMutator> {
        @Override
        public MathIsDivisibleByMutator newMutator() {
            return new MathIsDivisibleByMutator();
        }
    }

    private Block mBlock;
    private FieldDropdown mDropdown;

    private Input mDivisorInput =
            new Input.InputValue("DIVISOR", /* fields */ null, /* alignment */ null, NUMBER_CHECK);

    private List<Input> mInputsWithoutDivisor;
    private List<Input> mInputsWithDivisor;

    @Override
    public void onAttached(Block block) {
        if (mBlock != null) {
            throw new IllegalStateException("Cannot reuse mutator.");
        }
        mBlock = block;
        mDropdown = (FieldDropdown) block.getFieldByName("NUMBER_TO_CHECK");

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
    public void update(Block block, XmlPullParser parser)
            throws BlockLoadingException, IOException, XmlPullParserException {
        // Do nothing. No serialized state.
    }

    private void updateShape() {
        boolean isSelected = mDropdown.getSelectedValue().equals(DIVISIBLE_BY);
        boolean isShown = mDivisorInput.getBlock() != null;
        if (isSelected != isShown) {
            mBlock.reshape(
                    isSelected ? mInputsWithDivisor : mInputsWithoutDivisor,
                    mBlock.getOutputConnection(),
                    mBlock.getPreviousConnection(),
                    mBlock.getNextConnection());
        }
    }
}

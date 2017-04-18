package com.google.blockly.model;

import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.TestEventsCallback;
import com.google.blockly.utils.TestFieldObserver;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link Field} class.
 */

public class FieldTest extends BlocklyTestCase {
    static final String FIELD_NAME = "Field name";

    static final String INITIAL_VALUE = "initial value";
    static final String NEW_VALUE1 = "new value #1";
    static final String NEW_VALUE2 = "new value #2";

    BlocklyController mController;
    BlockFactory mFactory;
    Block mBlock;

    Field mField;

    TestEventsCallback mEventsCallback = new TestEventsCallback();
    TestFieldObserver mFieldObserver = new TestFieldObserver();

    @Before
    public void setUp() throws BlockLoadingException {
        configureForUIThread();

        mController = new BlocklyController.Builder(getContext())
                .build();
        mFactory = mController.getBlockFactory();
        mBlock = mFactory.obtainBlockFrom(new BlockTemplate().fromJson("{type:\"test block\"}"));

        mField = new FieldImpl(FIELD_NAME);
        Input input = new Input.InputDummy(
                null, Collections.singletonList(mField), Input.ALIGN_LEFT);
        mBlock.reshape(Collections.singletonList(input), null, null, null);

        mController.addCallback(mEventsCallback);
        mField.registerObserver(mFieldObserver);
    }

    @Test
    public void testFireValueChanged() {
        runAndSync(new Runnable() {
            @Override
            public void run() {
                assertThat(mEventsCallback.mEventsReceived).isEmpty();
                assertThat(mFieldObserver.mObservations).isEmpty();

                mField.setFromString(NEW_VALUE1);

                assertThat(mFieldObserver.mObservations).hasSize(1);
                assertThat(mFieldObserver.mObservations.get(0).mField).isSameAs(mField);
                assertThat(mFieldObserver.mObservations.get(0).mOldValue).isSameAs(INITIAL_VALUE);
                assertThat(mFieldObserver.mObservations.get(0).mNewValue).isSameAs(NEW_VALUE1);

                assertThat(mEventsCallback.mEventsReceived).isEmpty();  // Not attached

                mController.addRootBlock(mBlock);

                mEventsCallback.mEventsReceived.clear();
                mFieldObserver.mObservations.clear();

                mField.setFromString(NEW_VALUE2);

                assertThat(mFieldObserver.mObservations).hasSize(1);
                assertThat(mFieldObserver.mObservations.get(0).mField).isSameAs(mField);
                assertThat(mFieldObserver.mObservations.get(0).mOldValue).isSameAs(NEW_VALUE1);
                assertThat(mFieldObserver.mObservations.get(0).mNewValue).isSameAs(NEW_VALUE2);

                assertThat(mEventsCallback.mEventsReceived).hasSize(1);         // One event group
                assertThat(mEventsCallback.mEventsReceived.get(0)).hasSize(1);  // One event
                BlocklyEvent.ChangeEvent changeEvent =
                        (BlocklyEvent.ChangeEvent) mEventsCallback.mEventsReceived.get(0).get(0);
                assertThat(changeEvent.getElement()).isSameAs(BlocklyEvent.ELEMENT_FIELD);
                assertThat(changeEvent.getFieldName()).isEqualTo(FIELD_NAME);
                assertThat(changeEvent.getOldValue()).isEqualTo(NEW_VALUE1);
                assertThat(changeEvent.getNewValue()).isEqualTo(NEW_VALUE2);
            }
        });
    }

    class FieldImpl extends Field {
        String value = INITIAL_VALUE;

        public FieldImpl(String name) {
            super(name, TYPE_UNKNOWN);
        }

        @Override
        public boolean setFromString(String newValue) {
            String oldValue = value;
            value = newValue;
            fireValueChanged(oldValue, newValue);
            return true;
        }

        @Override
        public String getSerializedValue() {
            return value;
        }
    }
}

package com.google.blockly.utils;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.BlocklyController.EventsCallback;
import com.google.blockly.model.BlocklyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link EventsCallback} the stashes all events groups received from
 * {@link #onEventGroup} into {@link #mEventsReceived}.
 */
public class TestEventsCallback implements BlocklyController.EventsCallback {
    public final List<List<BlocklyEvent>> mEventsReceived = new ArrayList<>();

    @Override
    public int getTypesBitmask() {
        return BlocklyEvent.TYPE_ALL;
    }

    @Override
    public void onEventGroup(List<BlocklyEvent> events) {
        mEventsReceived.add(events);
    }
}

package com.google.blockly.android.demo;

import android.text.TextUtils;
import android.util.Log;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.control.BlocklyEvent;

import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Logs all {@link BlocklyEvent}s to the console.
 */
public class LogAllEventsCallback implements BlocklyController.EventsCallback {
    private static final String TAG = "LogAllEventsCallback";

    private final String mEventsTag;
    private boolean mPrettyPrint = false;

    public LogAllEventsCallback(String tag) {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        mEventsTag = tag;
    }

    public LogAllEventsCallback() {
        this(TAG);
    }

    public void setPrettyPrint(boolean prettyPrint) {
        mPrettyPrint = prettyPrint;
    }

    /**
     * @return {@link BlocklyEvent#TYPE_ALL} to listen for all events.
     */
    @Override
    public int getTypesBitmask() {
        return BlocklyEvent.TYPE_ALL;
    }

    @Override
    public void onEventGroup(List<BlocklyEvent> events) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        for (BlocklyEvent event : events) {
            try {
                pw.print(event.toJsonString());
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize event", e);
            }
        }
        pw.flush();
        Log.i(mEventsTag, sw.toString());

        try {
            pw.close();
            sw.close();
        } catch (IOException e) {
            // Shouldn't get here. Don't care anyway.
            Log.wtf(TAG, "Couldn't fire the writer. Broken pencils are pointless.");
        }
    }
}

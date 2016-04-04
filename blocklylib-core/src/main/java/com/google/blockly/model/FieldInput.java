package com.google.blockly.model;

import android.text.TextUtils;

import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Adds an editable text input to an Input.
 */
public final class FieldInput extends Field<FieldInput.Observer> {
    private String mText;

    public FieldInput(String name, String text) {
        super(name, TYPE_INPUT);
        mText = text;
    }

    public static FieldInput fromJson(JSONObject json) {
        // TODO: consider replacing default text with string resource
        return new FieldInput(
                json.optString("name", "NAME"),
                json.optString("text", "default"));
    }

    @Override
    public FieldInput clone() {
        return new FieldInput(getName(), mText);
    }

    @Override
    public boolean setFromString(String text) {
        setText(text);
        return true;
    }

    /**
     * @return The text the user has entered.
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the current text in this Field.
     *
     * @param text The text to replace the field content with.
     */
    public void setText(String text) {
        if (!TextUtils.equals(text, mText)) {
            String oldText = mText;
            mText = text;
            onTextChanged(oldText, text);
        }
    }

    @Override
    protected void serializeInner(XmlSerializer serializer) throws IOException {
        serializer.text(mText == null ? "" : mText);
    }

    private void onTextChanged(String oldText, String newText) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onTextChanged(this, oldText, newText);
        }
    }

    /**
     * Observer for listening to changes to an input field.
     */
    public interface Observer {
        /**
         * Called when the field's text changed.
         *
         * @param field The field that changed.
         * @param oldText The field's previous text.
         * @param newText The field's new text.
         */
        void onTextChanged(FieldInput field, String oldText, String newText);
    }
}

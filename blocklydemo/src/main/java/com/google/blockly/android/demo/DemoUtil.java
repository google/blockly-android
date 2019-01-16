package com.google.blockly.android.demo;

import android.content.Context;
import android.widget.TextView;

class DemoUtil {
    /**
     * Estimate the pixel size of the longest line of text, and set that to the TextView's minimum
     * width.
     */
    static void updateTextMinWidth(TextView generatedTextView, Context ctx) {
        String text = generatedTextView.getText().toString();
        int maxline = 0;
        int start = 0;
        int index = text.indexOf('\n', start);
        while (index > 0) {
            maxline = Math.max(maxline, index - start);
            start = index + 1;
            index = text.indexOf('\n', start);
        }
        int remainder = text.length() - start;
        if (remainder > 0) {
            maxline = Math.max(maxline, remainder);
        }

        float density = ctx.getResources().getDisplayMetrics().density;
        generatedTextView.setMinWidth((int) (maxline * 13 * density));
    }
}

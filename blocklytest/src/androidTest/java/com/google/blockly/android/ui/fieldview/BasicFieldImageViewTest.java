package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BasicFieldImageView}.
 */
public class BasicFieldImageViewTest {
    Context mMockContext;

    BasicFieldImageView mImageFieldView;

    @Before
    public void setUp() throws Exception {
        mMockContext = mock(Context.class, AdditionalAnswers.delegatesTo(InstrumentationRegistry.getContext()));

        mImageFieldView = new BasicFieldImageView(mMockContext);
    }

    @Test
    public void testImageSourceFromAssets() throws IOException {
        InputStream in = null;
        try {
            in = mImageFieldView.getStreamForSource("localIcon.png");
            assertNotEmpty(in);
            in.close();

            in = mImageFieldView.getStreamForSource("/localIcon.png");
            assertNotEmpty(in);
            in.close();

            in = mImageFieldView.getStreamForSource("file:///android_assets/localIcon.png");
            assertNotEmpty(in);
        } finally {
            in.close();
        }
    }

    private void assertNotEmpty(InputStream in) throws IOException {
        assertNotNull(in);
        assertTrue(in.available() > 0);
    }
}

package com.google.blockly.android.ui.fieldview;

import android.content.Context;

import com.google.blockly.android.MockitoAndroidTestCase;

import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for {@link BasicFieldImageView}.
 */
public class BasicFieldImageViewTest extends MockitoAndroidTestCase {
    Context mMockContext;

    BasicFieldImageView mImageFieldView;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mMockContext = Mockito.mock(Context.class, AdditionalAnswers.delegatesTo(getContext()));

        mImageFieldView = new BasicFieldImageView(mMockContext);
    }

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

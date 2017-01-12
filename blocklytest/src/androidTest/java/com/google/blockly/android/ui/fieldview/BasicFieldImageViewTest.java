package com.google.blockly.android.ui.fieldview;

import android.content.Context;

import com.google.blockly.android.BlocklyTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BasicFieldImageView}.
 */
public class BasicFieldImageViewTest extends BlocklyTestCase {
    Context mMockContext;

    BasicFieldImageView mImageFieldView;

    @Before
    public void setUp() throws Exception {
        mMockContext = mock(Context.class, AdditionalAnswers.delegatesTo(getContext()));

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
        assertThat(in).isNotNull();
        assertThat(in.available() > 0).isTrue();
    }
}

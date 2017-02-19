package com.google.blockly.android.codegen;

import android.os.Build;
import android.support.test.InstrumentationRegistry;

import com.google.blockly.android.test.R;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.IOOptions;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.blockly.model.BlockFactory.block;
import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for CodeGeneratorService
 */
public class CodeGeneratorServiceTest {
    private BlockFactory mBlockFactory;

    @Before
    public void setUp() throws Exception {
        mBlockFactory = new BlockFactory(InstrumentationRegistry.getContext(),
                new int[]{R.raw.test_blocks});
    }

    /**
     *
     * Since the serialized blocks are passed to the generator via a string in a JavaScript call,
     * all characters must be escaped correctly. Chromium is relaxed enough, we only really care
     * about the quote character.
     */
    @Test
    public void testEscapeFieldDataForChromium() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForAndroidWebView()
        }

        Block block = mBlockFactory.obtain(block().ofType("text"));
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml);

        Matcher matcher = Pattern.compile("javascript:generate\\('(.*)'\\);",
                Pattern.DOTALL | Pattern.MULTILINE).matcher(url);
        assertThat(matcher.matches()).isTrue();
        String jsString = matcher.group(1);
        assertThat(jsString.contains("apostrophe \\' end")).isTrue();
    }

    /**
     * Since the serialized blocks are passed to the generator via a string in a JavaScript call,
     * all characters must be escaped correctly. The pre-Chromium WebView requires a fully escaped
     * URL, using %20 for spaces (not +'s).
     */
    @Test
    public void testEscapeFieldDataForAndroidWebView() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForChromium()
        }

        Block block = mBlockFactory.obtain(block().ofType("text"));
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml);

        Matcher matcher = Pattern.compile("javascript:generateEscaped\\('(.*)'\\);").matcher(url);
        assertThat(matcher.matches()).isTrue();
        String jsString = matcher.group(1);
        assertThat(jsString.contains("apostrophe%20%27%20end")).isTrue();
    }

    private String toXml(Block block) {
        StringOutputStream out = new StringOutputStream();
        try {
            BlocklyXmlHelper.writeBlockToXml(block, out, null);
        } catch (BlocklySerializerException e) {
            throw new IllegalArgumentException("Failed to serialize block.", e);
        }
        return out.toString();
    }
}

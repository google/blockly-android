package com.google.blockly.android.codegen;

import android.os.Build;
import android.test.AndroidTestCase;

import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit tests for CodeGeneratorService
 */
public class CodeGeneratorServiceTest extends AndroidTestCase {
    private BlockFactory mBlockFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mBlockFactory = new BlockFactory(getContext(),
                new int[]{com.google.blockly.android.R.raw.test_blocks});
    }

    /**
     *
     * Since the serialized blocks are passed to the generator via a string in a JavaScript call,
     * all characters must be escaped correctly. Chromium is relaxed enough, we only really care
     * about the quote character.
     */
    public void testEscapeFieldDataForChromium() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForAndroidWebView()
        }

        Block block = mBlockFactory.obtainBlock("text", null);
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml);

        Matcher matcher = Pattern.compile("javascript:generate\\('(.*)'\\);",
                Pattern.DOTALL | Pattern.MULTILINE).matcher(url);
        assertTrue(matcher.matches());
        String jsString = matcher.group(1);
        assertTrue(jsString.contains("apostrophe \\' end"));
    }

    /**
     * Since the serialized blocks are passed to the generator via a string in a JavaScript call,
     * all characters must be escaped correctly. The pre-Chromium WebView requires a fully escaped
     * URL, using %20 for spaces (not +'s).
     */
    public void testEscapeFieldDataForAndroidWebView() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForChromium()
        }

        Block block = mBlockFactory.obtainBlock("text", null);
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml);

        Matcher matcher = Pattern.compile("javascript:generateEscaped\\('(.*)'\\);").matcher(url);
        assertTrue(matcher.matches());
        String jsString = matcher.group(1);
        assertTrue(jsString.contains("apostrophe%20%27%20end"));
    }

    private String toXml(Block block) {
        StringOutputStream out = new StringOutputStream();
        try {
            BlocklyXmlHelper.writeOneBlockToXml(block, out);
        } catch (BlocklySerializerException e) {
            throw new IllegalArgumentException("Failed to serialize block.", e);
        }
        return out.toString();
    }
}

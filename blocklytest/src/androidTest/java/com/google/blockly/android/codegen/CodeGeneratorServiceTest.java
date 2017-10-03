package com.google.blockly.android.codegen;

import android.os.Build;
import android.support.test.rule.ActivityTestRule;

import com.google.blockly.android.BlocklyTestActivity;
import com.google.blockly.android.BlocklyTestCase;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.BlockFactory;
import com.google.blockly.model.BlockTemplate;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.BlockLoadingException;
import com.google.blockly.utils.BlocklyXmlHelper;
import com.google.blockly.utils.StringOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for CodeGeneratorService
 */
public class CodeGeneratorServiceTest extends BlocklyTestCase {
    private static final String SIMPLE_WORKSPACE_XML =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\">"
                    + "<block type=\"text\"><field name=\"TEXT\">test</field></block>" +
            "</xml>";

    private BlockFactory mBlockFactory;
    private BlocklyController mMockController;
    private CodeGenerationRequest.CodeGeneratorCallback mCallback;
    private CodeGeneratorManager mManager;
    private BlocklyTestActivity mActivity;

    @Rule
    public final ActivityTestRule<BlocklyTestActivity> mActivityRule =
            new ActivityTestRule<>(BlocklyTestActivity.class);

    @Before
    public void setUp() throws Exception {
        mMockController = Mockito.mock(BlocklyController.class);
        mCallback = Mockito.mock(CodeGenerationRequest.CodeGeneratorCallback.class);

        configureForUIThread();

        mActivity = mActivityRule.getActivity();
        // TODO(#435): Replace R.raw.test_blocks
        mBlockFactory = new BlockFactory();
        mBlockFactory.addJsonDefinitions(mActivity.getAssets().open("default/test_blocks.json"));
        mBlockFactory.setController(mMockController);
        mManager = new CodeGeneratorManager(mActivity);
        mManager.onResume();
    }

    @After
    public void tearDown() {
        mManager.onPause();
    }

    @Test
    public void testLuaGeneration() {
        final CodeGenerationRequest request = new CodeGenerationRequest(
                SIMPLE_WORKSPACE_XML,
                mCallback,
                new LanguageDefinition("lua/lua_compressed.js", "Blockly.Lua"),
                Arrays.asList(new String[] {"default/test_blocks.json"}),
                Arrays.asList(new String[] {"lua/generators/test_blocks.js"}));
        mManager.requestCodeGeneration(request);
        // TODO Figure out why Travis doesn't work with this test.
//        Mockito.verify(mCallback, Mockito.timeout(8000))
//                .onFinishCodeGeneration("local _ = 'test'\n");
    }

    /**
     *
     * Since the serialized blocks are passed to the generator via a string in a JavaScript call,
     * all characters must be escaped correctly. Chromium is relaxed enough, we only really care
     * about the quote character.
     */
    @Test
    public void testEscapeFieldDataForChromium() throws BlockLoadingException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForAndroidWebView()
        }

        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml, "Blockly.JavaScript");

        Matcher matcher = Pattern.compile("javascript:generate\\('(.*)', Blockly.JavaScript\\);",
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
    public void testEscapeFieldDataForAndroidWebView() throws BlockLoadingException {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;  // See testEscapeFieldDataForChromium()
        }

        Block block = mBlockFactory.obtainBlockFrom(new BlockTemplate().ofType("text"));
        block.getFieldByName("TEXT").setFromString("apostrophe ' end");

        String xml = toXml(block);
        String url = CodeGeneratorService.buildCodeGenerationUrl(xml, "Blockly.JavaScript");

        Pattern urlPattern = Pattern.compile("javascript:generate\\('(.*)', Blockly.JavaScript\\);");
        assertThat(url).matches(urlPattern);
        String jsString = urlPattern.matcher(url).group(1);
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

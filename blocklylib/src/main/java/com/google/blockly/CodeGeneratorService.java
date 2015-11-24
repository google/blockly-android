/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by fenichel on 11/20/15.
 */
public class CodeGeneratorService extends Service {
    private static final String TAG = "CodeGeneratorService";

    public static final String EXTRA_WORKSPACE_XML = "com.google.blockly.WORKSPACE_XML";
    public static final String EXTRA_TOOLBOX_XML = "com.google.blockly.TOOLBOX_XML";

    WebView mWebview;
//    public CodeGeneratorService() {
//        super("CodeGeneratorService");
//    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //String blocklyXml = intent.getStringExtra(EXTRA_WORKSPACE_XML);

        final String blocklyXml = "<xml xmlns=\"http://www.w3.org/1999/xhtml\"> <block type=\"math_arithmetic\" x=\"-38\" y=\"-38\"><field name=\"OP\">ADD</field><value name=\"A\"><shadow type=\"math_number\"><field name=\"NUM\">1</field></shadow><block type=\"math_number\"><field name=\"NUM\">0</field></block></value><value name=\"B\"><shadow type=\"math_number\"><field name=\"NUM\">1</field></shadow><block type=\"math_number\"><field name=\"NUM\">1</field></block></value></block></xml>";

        //blockly = blockly.replace("'", "\\'");
        Log.e(TAG, "blockly:" + blocklyXml);
        mWebview = new WebView(this);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebview.addJavascriptInterface(new BlocklyJsObject(), "BlocklyController");

        /* WebViewClient must be set BEFORE calling loadUrl! */
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebview.loadUrl("javascript:generate('" + blocklyXml + "')");
            }
        });
        mWebview.loadUrl("file:///android_asset/index.html");
        return 1;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BlocklyJsObject {
        @JavascriptInterface
        public void execute(String program) {
            Log.d(TAG, "code: " + program);
        }
    }
}

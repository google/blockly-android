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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.blockly.utils.CodeGenerationRequest;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Background service that uses a WebView to statically load the Web Blockly libraries and use them
 * to generate code.
 */
public class CodeGeneratorService extends Service {
    private static final String TAG = "CodeGeneratorService";

    // Binder given to clients
    private final IBinder mBinder = new CodeGeneratorBinder();
    private final ArrayDeque<CodeGenerationRequest> mRequestQueue = new ArrayDeque<>();
    private boolean mReady = false;
    private WebView mWebview;
    private CodeGenerationRequest.CodeGeneratorCallback mCallback;
    private Handler mHandler;
    private String mBlockJsSource = "sample_sections/block_definitions.js";
    private String mBlockJSONSource = "sample_sections/block_definitions.json";
    private static final String ANDROID_ASSET_PREFIX = "file:///android_asset/";

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mWebview = new WebView(this);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setWebChromeClient(new WebChromeClient());

        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                Log.d(TAG, url);
                if (url.equals("http://android_asset/sample_sections/block_definitions.json")) {
                    try {
                        WebResourceResponse response =
                                new WebResourceResponse("text/javascript", "UTF-8",
                                        getAssets().open(mBlockJSONSource));
                                        // Add a "var jsonArr = " to the front of the file.
//                                        new SequenceInputStream(
//                                                new ByteArrayInputStream(
//                                                        "var jsonArr = ".getBytes()),
//                                                getAssets().open(mBlockJSONSource)));

                        Map<String, String> responseHeaders = new HashMap<>();
                        responseHeaders.put("Access-Control-Allow-Origin", "*");
                        response.setResponseHeaders(responseHeaders);
                        return response;
                    } catch (IOException e) {
                        Log.d(TAG, "Couldn't read file");
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view,
                    WebResourceRequest request) {
                Log.d(TAG, "hineini");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return shouldInterceptRequest(view, request.getUrl().toString());
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                synchronized (this) {
                    mReady = true;
                }
            generateCode();
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebview.addJavascriptInterface(new BlocklyJsObject(), "BlocklyController");

        /* WebViewClient must be set BEFORE calling loadUrl! */
//        mWebview.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                synchronized (this) {
//                    mReady = true;
//                }
//                generateCode();
//            }
//        });
        mWebview.loadUrl(ANDROID_ASSET_PREFIX + "index.html");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Enqueues a {@link CodeGenerationRequest} and kicks off generation of the first request in the
     * queue if no request is in progress.
     *
     * @param request The request to add to the queue.
     */
    public void requestCodeGeneration(CodeGenerationRequest request) {
        synchronized (this) {
            mRequestQueue.add(request);
        }
        generateCode();
    }

    /**
     * If no {@link CodeGenerationRequest} instances are already being processed, kicks off
     * generation of code for the first request in the queue.
     */
    private void generateCode() {
        synchronized (this) {
            if (mReady && mRequestQueue.size() > 0) {
                mReady = false;
                final CodeGenerationRequest request = mRequestQueue.pop();
                // Run on the main thread.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback = request.getCallback();
                        mWebview.loadUrl("javascript:generate('" + request.getXml() + "')");
                    }
                });
            }
        }
    }

    private class BlocklyJsObject {
        @JavascriptInterface
        public void execute(String program) {
            mReady = true;
            if (mCallback != null) {
                mCallback.onFinishCodeGeneration(program);
            }
            generateCode();
        }
    }

    public class CodeGeneratorBinder extends Binder {
        CodeGeneratorService getService() {
            return CodeGeneratorService.this;
        }
    }
}

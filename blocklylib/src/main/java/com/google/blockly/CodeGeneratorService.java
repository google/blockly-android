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
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.blockly.utils.CodeGenerationRequest;

import java.util.ArrayDeque;

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

    @Override
    public void onCreate() {
        mHandler = new Handler();
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
                synchronized (this) {
                    mReady = true;
                }
                generateCode();
            }
        });
        mWebview.loadUrl("file:///android_asset/index.html");
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

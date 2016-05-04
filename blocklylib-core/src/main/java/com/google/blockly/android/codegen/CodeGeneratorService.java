/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.codegen;

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
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Background service that uses a WebView to statically load the Web Blockly libraries and use them
 * to generate code.
 */
public class CodeGeneratorService extends Service {
    private static final String TAG = "CodeGeneratorService";
    private static final String BLOCKLY_COMPILER_PAGE =
            "file:///android_asset/background_compiler.html";

    // Binder given to clients
    private final IBinder mBinder = new CodeGeneratorBinder();
    private final ArrayDeque<CodeGenerationRequest> mRequestQueue = new ArrayDeque<>();
    private boolean mReady = false;
    private WebView mWebview;
    private CodeGenerationRequest.CodeGeneratorCallback mCallback;
    private Handler mHandler;
    private List<String> mDefinitions = new ArrayList<>();
    private List<String> mGenerators = new ArrayList<>();
    private String mAllBlocks;

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mWebview = new WebView(this);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebview.addJavascriptInterface(new BlocklyJavascriptInterface(),
                "BlocklyJavascriptInterface");

        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                synchronized (this) {
                    mReady = true;
                }
                handleRequest();
            }
        });
        mWebview.loadUrl(BLOCKLY_COMPILER_PAGE);
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
        handleRequest();
    }

    /**
     * If no {@link CodeGenerationRequest} instances are already being processed, kicks off
     * generation of code for the first request in the queue.
     */
    private void handleRequest() {
        synchronized (this) {
            if (mReady && mRequestQueue.size() > 0) {
                mReady = false;
                final CodeGenerationRequest request = mRequestQueue.pop();
                // Run on the main thread.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback = request.getCallback();
                        if (!equivalentLists(request.getBlockDefinitionsFilenames(), mDefinitions)
                                || !equivalentLists(request.getBlockGeneratorsFilenames(),
                                mGenerators)) {
                             //Reload the page with the new block definitions.  Push the request
                             //back onto the queue until the page is loaded.
                            mDefinitions = request.getBlockDefinitionsFilenames();
                            mGenerators = request.getBlockGeneratorsFilenames();
                            mAllBlocks = null;
                            mRequestQueue.addFirst(request);
                            mWebview.loadUrl(BLOCKLY_COMPILER_PAGE);
                        } else {
                            mWebview.loadUrl("javascript:" +
                                    "generate('" + request.getXml() + "')");
                        }
                    }
                });
            }
        }
    }

    private class BlocklyJavascriptInterface {
        @JavascriptInterface
        public void execute(String program) {
            CodeGenerationRequest.CodeGeneratorCallback cb;
            synchronized (this) {
                cb = mCallback;
                mReady = true;
            }
            if (cb != null) {
                cb.onFinishCodeGeneration(program);
            }
            handleRequest();
        }

        @JavascriptInterface
        public String getBlockGeneratorsFilenames() {
            if (mGenerators == null || mGenerators.size() == 0) {
                return "";
            }
            StringBuilder combined = new StringBuilder(mGenerators.get(0));
            for (int i = 1; i < mGenerators.size(); i++) {
                combined.append(";");
                combined.append(mGenerators.get(i));
            }
            return combined.toString();
        }

        @JavascriptInterface
        public String getBlockDefinitions() {
            if (mAllBlocks != null) {
                return mAllBlocks;
            }
            if (mDefinitions.isEmpty()) {
                return "";
            }
            if (mDefinitions.size() == 1) {
                // Pass in contents without parsing.
                String filename = mDefinitions.get(0);
                try {
                    return loadAssetAsUtf8(filename);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't find block definitions file \"" + filename + "\"");
                    return "";
                }
            } else {
                // Concatenate all definitions into a single stream.
                JSONArray allBlocks = new JSONArray();
                String filename = null;
                try {
                    if (mDefinitions != null) {
                        Iterator<String> iter = mDefinitions.iterator();
                        while (iter.hasNext()) {
                            filename = iter.next();
                            String contents = loadAssetAsUtf8(filename);
                            JSONArray fileBlocks = new JSONArray(contents);
                            for (int i = 0; i < fileBlocks.length(); ++i) {
                                allBlocks.put(fileBlocks.getJSONObject(i));
                            }
                        }
                    }
                } catch (IOException|JSONException e) {
                    Log.e(TAG, "Error reading block definitions file \"" + filename + "\"");
                    return "";
                }
                mAllBlocks = allBlocks.toString();
                return mAllBlocks;
            }
        }
    }

    public class CodeGeneratorBinder extends Binder {
        public CodeGeneratorService getService() {
            return CodeGeneratorService.this;
        }
    }

    private boolean equivalentLists(List<String> newDefinitions, List<String> oldDefinitions) {
        LinkedList<String> checkList = new LinkedList<>(oldDefinitions);
        for (String filename : newDefinitions) {
            if (!checkList.remove(filename)) {
                return false;
            }
        }
        return checkList.isEmpty(); // If it is empty, all filenames were found / matched.
    }

    private String loadAssetAsUtf8(String filename) throws IOException {
        InputStream input = null;
        try {
            input = getAssets().open(filename);

            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);

            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new IllegalArgumentException("Couldn't find asset file \"" + mDefinitions + "\"");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.w(TAG, "Unable to close asset file \"" + filename + "\"", e);
                }
            }
        }
    }
}

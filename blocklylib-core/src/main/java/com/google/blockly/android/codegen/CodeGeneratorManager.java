/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Client Side class responsible for connecting to the {@link CodeGeneratorService}.
 *
 * A connection to the service is only made the first time code generation is requested.
 */
public class CodeGeneratorManager {
    private static final String TAG = "CodeGeneratorManager";

    private final Context mContext;
    private final Queue<CodeGenerationRequest> mStoredRequests;

    private final ServiceConnection mCodeGenerationConnection;
    private CodeGeneratorService mGeneratorService;

    private boolean mResumed = false;
    private boolean mIsConnecting = false;

    public CodeGeneratorManager(Context context) {
        this.mContext = context;
        this.mStoredRequests = new LinkedList<>();

        this.mCodeGenerationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                try {
                    if (!mResumed) {
                        unbind();
                    } else {
                        mGeneratorService = ((CodeGeneratorService.CodeGeneratorBinder) binder)
                                .getService();

                        while (!mStoredRequests.isEmpty()) {
                            executeCodeGenerationRequest(mStoredRequests.poll());
                        }
                    }
                } finally {
                    mIsConnecting = false;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mGeneratorService = null;
            }
        };
    }

    /**
     * Unbind the underlying service (if it is bound).
     */
    public void onPause() {
        mResumed = false;
        unbind();
    }

    /**
     * Inform this class that it is ok to bind to the service and remove any stored requests as
     * the service won't be bound until a new request comes in.
     */
    public void onResume() {
        mResumed = true;
        mStoredRequests.clear();
    }

    /**
     * Checks if the service is currently bound and unbinds it if it is.
     */
    protected void unbind() {
        if (isBound()) {
            mContext.unbindService(mCodeGenerationConnection);
            mGeneratorService = null;
        }
    }

    /**
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param codeGenerationRequest the request to generate code.
     */
    public void requestCodeGeneration(CodeGenerationRequest codeGenerationRequest) {
        if(!mResumed) {
            Log.w(TAG, "Code generation called while paused. Request ignored.");
            return;
        }
        if (codeGenerationRequest == null) {
            Log.w(TAG, "codeGenerationRequest was null");
            return;
        }
        if (isBound()) {
            executeCodeGenerationRequest(codeGenerationRequest);
        } else {
            mStoredRequests.add(codeGenerationRequest);
            if (!mIsConnecting) {
                connectToService();
            }
        }
    }

    private boolean isBound() {
        return mGeneratorService != null;
    }

    private void connectToService() {
        mIsConnecting = true;
        Intent intent = new Intent(mContext, CodeGeneratorService.class);
        mContext.bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
    }

    private void executeCodeGenerationRequest(CodeGenerationRequest request) {
        if (request != null) {
            mGeneratorService.requestCodeGeneration(request);
        }
    }
}

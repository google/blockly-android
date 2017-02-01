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
                if (!mResumed) {
                    mContext.unbindService(mCodeGenerationConnection);
                } else {
                    mGeneratorService = ((CodeGeneratorService.CodeGeneratorBinder) binder).getService();
                    mIsConnecting = false;

                    while (!mStoredRequests.isEmpty()) {
                        executeCodeGenerationRequest(mStoredRequests.poll());
                    }
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
        if (isBound()) {
            mContext.unbindService(mCodeGenerationConnection);
        }
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
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param codeGenerationRequest the request to generate code.
     */
    public void requestCodeGeneration(CodeGenerationRequest codeGenerationRequest) {
        if (isBound()) {
            if(!mResumed) {
                Log.w(TAG, "Code generation called while paused. Request ignored.");
                return;
            }
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

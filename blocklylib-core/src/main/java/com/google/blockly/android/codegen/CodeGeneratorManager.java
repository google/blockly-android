package com.google.blockly.android.codegen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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

    public CodeGeneratorManager(Context context) {
        this.mContext = context;
        this.mStoredRequests = new LinkedList<>();

        this.mCodeGenerationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                mGeneratorService = ((CodeGeneratorService.CodeGeneratorBinder) binder).getService();
                while (!mStoredRequests.isEmpty()) {
                    executeCodeGenerationRequest(mStoredRequests.poll());
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
        if (isBound()) {
            mContext.unbindService(mCodeGenerationConnection);
        }
    }

    /**
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param codeGenerationRequest the request to generate code.
     */
    public void requestCodeGeneration(CodeGenerationRequest codeGenerationRequest) {
        if (isBound()) {
            executeCodeGenerationRequest(codeGenerationRequest);
        } else {
            mStoredRequests.add(codeGenerationRequest);
            connectToService();
        }
    }

    private boolean isBound() {
        return mGeneratorService != null;
    }

    private void connectToService() {
        Intent intent = new Intent(mContext, CodeGeneratorService.class);
        mContext.bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
    }

    private void executeCodeGenerationRequest(CodeGenerationRequest request) {
        if (request != null) {
            mGeneratorService.requestCodeGeneration(request);
        }
    }
}

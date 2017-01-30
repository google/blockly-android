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

    private ServiceConnection mCodeGenerationConnection;
    private CodeGeneratorService mGeneratorService;

    public CodeGeneratorManager(Context context) {
        this.mContext = context;
        this.mStoredRequests = new LinkedList<>();
    }

    /**
     * Resumes the underlying code generation connection.
     */
    public void onResume() {
        Intent intent = new Intent(mContext, CodeGeneratorService.class);
        if (mCodeGenerationConnection != null) {
            mContext.bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Pauses the underlying code generation connection.
     */
    public void onPause() {
        if (mCodeGenerationConnection != null) {
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

    private void executeCodeGenerationRequest(CodeGenerationRequest request) {
        if (request != null) {
            mGeneratorService.requestCodeGeneration(request);
        }
    }
}

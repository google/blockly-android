package com.google.blockly.android.codegen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.blockly.android.codegen.CodeGeneratorService.CodeGeneratorBinder;

class LazyServiceConnection {

    private final Context mContext;
    private ServiceConnection mCodeGenerationConnection;

    private CodeGeneratorService mGeneratorService;
    private CodeGenerationRequest mStoredRequest;

    LazyServiceConnection(Context context) {
        this.mContext = context;
    }

    void requestCodeGeneration(CodeGenerationRequest codeGenerationRequest) {
        if (isBound()) {
            executeCodeGenerationRequest(codeGenerationRequest);
        } else {
            mStoredRequest = codeGenerationRequest;
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
                mGeneratorService = ((CodeGeneratorBinder) binder).getService();
                executeCodeGenerationRequest(mStoredRequest);
                mStoredRequest = null;
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

    void onResume() {
        Intent intent = new Intent(mContext, CodeGeneratorService.class);
        if (mCodeGenerationConnection != null) {
            mContext.bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
        }
    }

    void onPause() {
        if (mCodeGenerationConnection != null) {
            mContext.unbindService(mCodeGenerationConnection);
        }
    }
}

package com.google.blockly.android.codegen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.blockly.android.codegen.CodeGenerationRequest.CodeGeneratorCallback;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.StringOutputStream;

import java.util.List;

/**
 * Client Side class responsible for connecting to the {@link CodeGeneratorService}.
 */
public class CodeGeneratorManager {
    private static final String TAG = "CodeGeneratorManager";

    private final Context mContext;
    private final ServiceConnection mCodeGenerationConnection;

    private CodeGeneratorService mGeneratorService;

    public CodeGeneratorManager(Context context) {
        this.mContext = context;

        this.mCodeGenerationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                mGeneratorService =
                    ((CodeGeneratorService.CodeGeneratorBinder) binder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mGeneratorService = null;
            }
        };
    }

    public void onResume() {
        Intent intent = new Intent(mContext, CodeGeneratorService.class);
        mContext.bindService(intent, mCodeGenerationConnection, Context.BIND_AUTO_CREATE);
    }

    public void onPause() {
        mContext.unbindService(mCodeGenerationConnection);
    }

    /**
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param request The information required to perform code generation.
     */
    public void requestCodeGeneration(CodeGenerationRequest request) {
        if (isBound()) {
            mGeneratorService.requestCodeGeneration(request);
        } else {
            Log.i(TAG, "Generator not bound to service. Skipping run request.");
        }
    }

    private boolean isBound() {
        return mGeneratorService != null;
    }
}

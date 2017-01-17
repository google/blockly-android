package com.google.blockly.android.codegen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

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
     * @param workspace the workspace to request code generation for.
     * @param codeGenerationCallback the callback to call with the generated code.
     * @throws BlocklySerializerException if the workspace cannot be serialized to XML.
     */
    public void requestCodeGeneration(List<String> blockDefinitionsJsonPaths,
                                      List<String> generatorsJsPaths,
                                      Workspace workspace,
                                      CodeGeneratorCallback codeGenerationCallback)
        throws BlocklySerializerException {

        if (workspace.hasBlocks()) {
            if (isBound()) {
                final StringOutputStream serialized = new StringOutputStream();
                workspace.serializeToXml(serialized);

                mGeneratorService.requestCodeGeneration(
                    new CodeGenerationRequest(
                        serialized.toString(),
                        codeGenerationCallback,
                        blockDefinitionsJsonPaths,
                        generatorsJsPaths)
                );
            } else {
                Log.i(TAG, "Generator not bound to service. Skipping run request.");
            }
        } else {
            Log.i(TAG, "No blocks in workspace. Skipping run request.");
        }
    }

    private boolean isBound() {
        return mGeneratorService != null;
    }
}

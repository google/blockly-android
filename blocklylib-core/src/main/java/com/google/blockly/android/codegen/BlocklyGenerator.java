package com.google.blockly.android.codegen;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.blockly.android.WorkspaceFragment;
import com.google.blockly.android.codegen.CodeGenerationRequest.CodeGeneratorCallback;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.StringOutputStream;

import java.util.List;

/**
 * Client Side class responsible for connecting to the {@link BlocklyGeneratorService}.
 */
public class BlocklyGenerator {
    private static final String TAG = "BlocklyGenerator";

    private final ServiceConnection mCodeGenerationConnection;
    private final List<String> mBlockDefinitionsJsonPaths;
    private final List<String> mGeneratorsJsPaths;

    private BlocklyGeneratorService mGeneratorService;

    public BlocklyGenerator(List<String> blockDefinitionsJsonPaths,
                            List<String> generatorsJsPaths) {
        this.mBlockDefinitionsJsonPaths = blockDefinitionsJsonPaths;
        this.mGeneratorsJsPaths = generatorsJsPaths;

        this.mCodeGenerationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                mGeneratorService =
                    ((BlocklyGeneratorService.CodeGeneratorBinder) binder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mGeneratorService = null;
            }
        };
    }

    public ServiceConnection getCodeGenerationService() {
        return mCodeGenerationConnection;
    }

    /**
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param workspaceFragment the workspace to request code generation for.
     * @param codeGenerationCallback the callback to call with the generated code.
     * @throws BlocklySerializerException if the workspace cannot be serialized to XML.
     */
    public void requestCodeGeneration(WorkspaceFragment workspaceFragment,
                                      CodeGeneratorCallback codeGenerationCallback)
        throws BlocklySerializerException {

        if (workspaceFragment.hasBlocks()) {
            if (isBound()) {
                final StringOutputStream serialized = new StringOutputStream();
                workspaceFragment.getWorkspace().serializeToXml(serialized);

                mGeneratorService.requestCodeGeneration(
                    new CodeGenerationRequest(
                        serialized.toString(),
                        codeGenerationCallback,
                        mBlockDefinitionsJsonPaths,
                        mGeneratorsJsPaths)
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

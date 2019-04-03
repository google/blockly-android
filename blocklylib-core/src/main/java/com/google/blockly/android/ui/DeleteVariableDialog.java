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

package com.google.blockly.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.utils.LangUtils;

/**
 * Default dialog window shown when deleting a variable in the workspace.
 */
public class DeleteVariableDialog extends DialogFragment {
    private String mVariable;
    private int mCount;
    private BlocklyController mController;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceBundle) {
        AlertDialog.Builder bob = new AlertDialog.Builder(getActivity());
        bob.setTitle(LangUtils.interpolate("%{BKY_DELETE_VARIABLE}")
                .replace("%1", mVariable));
        bob.setMessage(LangUtils.interpolate("%{BKY_DELETE_VARIABLE_CONFIRMATION}")
                .replace("%1", String.valueOf(mCount)).replace("%2", mVariable));
        bob.setPositiveButton(LangUtils.interpolate("%{BKY_IOS_VARIABLES_DELETE_BUTTON}"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mController.deleteVariable(mVariable);
                    }
                });
        return bob.create();
    }

    public void setController(BlocklyController controller) {
        mController = controller;
    }

    public void setVariable(String variable, int count) {
        mVariable = variable;
        mCount = count;
    }
}

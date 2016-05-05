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

package com.google.blockly.android;

import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.FieldAngle;
import com.google.blockly.model.FieldCheckbox;
import com.google.blockly.model.FieldColor;
import com.google.blockly.model.FieldDate;
import com.google.blockly.model.FieldDropdown;
import com.google.blockly.model.FieldImage;
import com.google.blockly.model.FieldInput;
import com.google.blockly.model.FieldLabel;
import com.google.blockly.model.Input;
import com.google.blockly.model.Workspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Source of test blocks for working on views and interactions.
 */
public final class MockBlocksProvider {
    private MockBlocksProvider() {
    }

    public static Block makeDummyBlock() {
        Block.Builder bob = new Block.Builder("dummy");
        bob.setPosition(5, 5);

        bob.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
        bob.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

        Input input = new Input.InputValue("input2", null, null);
        input.add(new FieldLabel("checkbox?", "this is a checkbox:"));
        input.add(new FieldCheckbox("checkbox!", true));
        bob.addInput(input);

        input = new Input.InputDummy("input1", null);
        input.add(new FieldLabel("label", "degrees"));
        bob.addInput(input);

        input = new Input.InputValue("input3", Input.ALIGN_CENTER, null);
        input.add(new FieldAngle("label2", 180));
        input.add(new FieldDate("date!", "2015-03-19"));
        FieldDropdown dropdown = new FieldDropdown("dropdown");
        dropdown.setOptions(
                Arrays.asList(new String[]{"value1", "value2"}),
                Arrays.asList(new String[]{"option1", "option2"}));
        input.add(dropdown);
        bob.addInput(input);

        input = new Input.InputStatement("input6", null, null);
        input.add(new FieldInput("DO", "loop"));
        bob.addInput(input);

        input = new Input.InputValue("input7", Input.ALIGN_RIGHT, null);
        input.add(new FieldImage(
                "image", "https://www.gstatic.com/codesite/ph/images/star_on.gif", 15, 15, "star"));
        input.add(new FieldColor("color", 0xFF0000));
        bob.addInput(input);

        input = new Input.InputValue("input8", null, null);
        input.add(new FieldInput("input text", "initial wide field of text"));
        bob.addInput(input);

        input = new Input.InputStatement("input9", Input.ALIGN_RIGHT, null);
        input.add(new FieldInput("DO", "another loop"));
        bob.addInput(input);

        bob.setColorHue(40);
        return bob.build();
    }

    public static Block makeOuterBlock() {
        Block.Builder block = new Block.Builder("outer");
        block.setPosition(400, 70);

        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input1", null);
        input.add(new FieldLabel("label", "block"));
        block.addInput(input);

        input = new Input.InputValue("input2", null, null);
        input.add(new FieldLabel("label", "value"));
        block.addInput(input);

        input = new Input.InputStatement("input3", null, null);
        input.add(new FieldLabel("DO", "this is a loop"));
        block.addInput(input);

        input = new Input.InputValue("input4", null, null);
        input.add(new FieldLabel("label", "another value"));
        block.addInput(input);

        return block.build();
    }

    public static Block makeStatementInputBlock() {
        Block.Builder block = new Block.Builder("statement-only");
        block.setPosition(500, 370);

        Input input = new Input.InputStatement("input", null, null);
        input.add(new FieldLabel("DO", "this is a loop"));
        block.addInput(input);

        return block.build();
    }

    public static Block makeInnerBlock() {
        Block.Builder block = new Block.Builder("inner");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input1", Input.ALIGN_RIGHT);
        input.add(new FieldLabel("label", "block"));
        block.addInput(input);

        input = new Input.InputValue("input2", null, null);
        input.add(new FieldLabel("label", "value"));
        block.addInput(input);

        input = new Input.InputValue("input3", null, null);
        input.add(new FieldLabel("label", "another value"));
        block.addInput(input);

        block.setColorHue(120);
        return block.build();
    }

    public static Block makeSimpleValueBlock() {
        Block.Builder block = new Block.Builder("simpleValue");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input", null);
        input.add(new FieldLabel("label", "zero"));
        block.addInput(input);

        block.setColorHue(210);
        return block.build();
    }

    public static Block makeValueInputBlock() {
        Block.Builder block = new Block.Builder("valueInput");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputValue("input", null, null);
        input.add(new FieldLabel("label", "one input"));
        block.addInput(input);

        block.setColorHue(120);
        return block.build();
    }

    public static Block makeStatementBlock() {
        Block.Builder block = new Block.Builder("statement");
        block.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
        block.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

        Input input = new Input.InputDummy("input", null);
        input.add(new FieldLabel("label", "do something"));
        block.addInput(input);

        block.setColorHue(240);
        return block.build();
    }

    /**
     * For use in unit tests.  If you want to make changes for live testing, make them in
     * {@link #makeComplexModel}
     */
    public static List<Block> makeTestModel() {
        List<Block> rootBlocks = new ArrayList<>();

        Block parent = makeDummyBlock();
        Block ivb = makeValueInputBlock();
        parent.getInputs().get(0).getConnection().connect(ivb.getOutputConnection());
        Block svb = makeSimpleValueBlock();
        ivb.getInputs().get(0).getConnection().connect(svb.getOutputConnection());
        Block smb = makeStatementBlock();
        parent.getInputs().get(3).getConnection().connect(smb.getPreviousConnection());

        Block child = makeDummyBlock();
        child.setInputsInline(true);
        child.getPreviousConnection().connect(parent.getNextConnection());

        Block third = makeDummyBlock();
        third.setInputsInline(true);
        third.getPreviousConnection().connect(child.getNextConnection());

        smb = makeStatementBlock();
        child.getInputs().get(3).getConnection().connect(smb.getPreviousConnection());
        rootBlocks.add(parent);     // Recursively adds all of its children.

        Block outerBlock = makeOuterBlock();
        outerBlock.setInputsInline(true);
        Block innerBlock = makeInnerBlock();
        outerBlock.getInputs().get(1).getConnection().connect(innerBlock.getOutputConnection());
        Block ivb2 = makeValueInputBlock();
        innerBlock.getInputs().get(2).getConnection().connect(ivb2.getOutputConnection());
        rootBlocks.add(outerBlock);

        rootBlocks.add(makeStatementInputBlock());

        return rootBlocks;
    }

    public static void airstrike(int numBlocks, Workspace workspace) {
        Block dummyBlock;
        for (int i = 0; i < numBlocks; i++) {
            dummyBlock = makeDummyBlock();
            int randomX = (int) (Math.random() * 5000);
            int randomY = (int) (Math.random() * 5000);
            dummyBlock.setPosition(randomX, randomY);

            workspace.addRootBlock(dummyBlock, true);
        }
    }

    public static void makeComplexModel(BlocklyController controller) {
        List<Block> testRootBlocks = makeTestModel();
        for (int i = 0; i < testRootBlocks.size(); i++) {
            controller.addRootBlock(testRootBlocks.get(i));
        }
    }

    public static void addDefaultVariables(BlocklyController controller) {
        controller.addVariable("item");
        controller.addVariable("zim");
        controller.addVariable("gir");
        controller.addVariable("tak");
    }

    /**
     * Add many complex blocks to the workspace, with each connected to a statement input of the
     * one above, to test our ability to render large blocks and to drag block groups with many
     * connections.
     *
     * @param numBlocks How many blocks to add.
     * @param workspace The workspace to add blocks to.
     */
    public static void spaghettiManyConnections(int numBlocks, Workspace workspace) {
        Block rootBlock = makeDummyBlock();
        Block prev = rootBlock;
        for (int i = 0; i < numBlocks; i++) {
            Block next = makeDummyBlock();
            next.getPreviousConnection().connect(prev.getInputByName("input6").getConnection());
            prev = next;
        }
        workspace.addRootBlock(rootBlock, true);
    }
}

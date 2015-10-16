/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.Workspace;

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
        input.add(new Field.FieldLabel("checkbox?", "this is a checkbox:"));
        input.add(new Field.FieldCheckbox("checkbox!", true));
        bob.addInput(input);

        input = new Input.InputDummy("input1", null);
        input.add(new Field.FieldLabel("label", "degrees"));
        bob.addInput(input);

        input = new Input.InputValue("input3", Input.ALIGN_CENTER, null);
        input.add(new Field.FieldAngle("label2", 180));
        input.add(new Field.FieldDate("date!", "2015-03-19"));
        input.add(new Field.FieldDropdown("dropdown", new String[]{"option1", "option2"},
                new String[]{"value1", "value2"}));
        bob.addInput(input);

        input = new Input.InputStatement("input6", null, null);
        input.add(new Field.FieldInput("DO", "loop"));
        bob.addInput(input);

        input = new Input.InputValue("input7", Input.ALIGN_RIGHT, null);
        input.add(new Field.FieldImage(
                "image", "https://www.gstatic.com/codesite/ph/images/star_on.gif", 15, 15, "star"));
        input.add(new Field.FieldColour("color", 0xFF0000));
        bob.addInput(input);

        input = new Input.InputValue("input8", null, null);
        input.add(new Field.FieldInput("input text", "initial wide field of text"));
        bob.addInput(input);

        input = new Input.InputStatement("input9", Input.ALIGN_RIGHT, null);
        input.add(new Field.FieldInput("DO", "another loop"));
        bob.addInput(input);

        bob.setColour(42);
        return bob.build();
    }

    public static Block makeOuterBlock() {
        Block.Builder block = new Block.Builder("outer");
        block.setPosition(400, 70);

        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input1", null);
        input.add(new Field.FieldLabel("label", "block"));
        block.addInput(input);

        input = new Input.InputValue("input2", null, null);
        input.add(new Field.FieldLabel("label", "value"));
        block.addInput(input);

        input = new Input.InputStatement("input3", null, null);
        input.add(new Field.FieldLabel("DO", "this is a loop"));
        block.addInput(input);

        input = new Input.InputValue("input4", null, null);
        input.add(new Field.FieldLabel("label", "another value"));
        block.addInput(input);

        return block.build();
    }

    public static Block makeInnerBlock() {
        Block.Builder block = new Block.Builder("inner");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input1", Input.ALIGN_RIGHT);
        input.add(new Field.FieldLabel("label", "block"));
        block.addInput(input);

        input = new Input.InputValue("input2", null, null);
        input.add(new Field.FieldLabel("label", "value"));
        block.addInput(input);

        input = new Input.InputValue("input3", null, null);
        input.add(new Field.FieldLabel("label", "another value"));
        block.addInput(input);

        block.setColour(120);
        return block.build();
    }

    public static Block makeSimpleValueBlock() {
        Block.Builder block = new Block.Builder("simpleValue");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputDummy("input", null);
        input.add(new Field.FieldLabel("label", "zero"));
        block.addInput(input);

        block.setColour(210);
        return block.build();
    }

    public static Block makeValueInputBlock() {
        Block.Builder block = new Block.Builder("valueInput");
        block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

        Input input = new Input.InputValue("input", null, null);
        input.add(new Field.FieldLabel("label", "one input"));
        block.addInput(input);

        block.setColour(110);
        return block.build();
    }

    public static Block makeStatementBlock() {
        Block.Builder block = new Block.Builder("statement");
        block.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
        block.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

        Input input = new Input.InputDummy("input", null);
        input.add(new Field.FieldLabel("label", "do something"));
        block.addInput(input);

        block.setColour(240);
        return block.build();
    }

    public static void makeTestModel(Workspace workspace) {
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
        workspace.addRootBlock(parent);    // Recursively adds all of its children.

        Block outerBlock = makeOuterBlock();
        outerBlock.setInputsInline(true);
        Block innerBlock = makeInnerBlock();
        outerBlock.getInputs().get(1).getConnection().connect(innerBlock.getOutputConnection());
        Block ivb2 = makeValueInputBlock();
        innerBlock.getInputs().get(2).getConnection().connect(ivb2.getOutputConnection());
        workspace.addRootBlock(outerBlock);
        //airstrike(10, workspace);
    }

    public static void airstrike(int numBlocks, Workspace workspace) {
        Block dummyBlock;
        for (int i = 0; i < numBlocks; i++) {
            dummyBlock = makeDummyBlock();
            int randomX = (int) (Math.random() * 250);
            int randomY = (int) (Math.random() * 500);
            dummyBlock.setPosition(randomX, randomY);

            workspace.addRootBlock(dummyBlock);
        }
    }
}

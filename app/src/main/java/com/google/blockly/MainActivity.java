/*
 * Copyright  2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.blockly.model.Block;
import com.google.blockly.model.Connection;
import com.google.blockly.model.Field;
import com.google.blockly.model.Input;
import com.google.blockly.model.Workspace;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.BlockView;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        Workspace mWorkspace;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);
            WorkspaceView wv = (WorkspaceView) rootView.findViewById(R.id.workspace);
            wv.setWorkspace(mWorkspace);
            WorkspaceHelper helper = mWorkspace.getWorkspaceHelper();
            Block dummyBlock = makeDummyBlock();
            mWorkspace.addRootBlock(dummyBlock);
            BlockGroup bg = new BlockGroup(getActivity(), helper);
            BlockView bv = helper.obtainBlockView(dummyBlock);
            bg.addView(bv);

            Block ivb = makeValueInputBlock();
            BlockView ivv = helper.obtainBlockView(ivb);
            bv.getInputView(0).setChildView(ivv);

            Block svb = makeSimpleValueBlock();
            BlockView svv = helper.obtainBlockView(svb);
            ivv.getInputView(0).setChildView(svv);

            Block smb = makeStatementBlock();
            BlockView smbv = helper.obtainBlockView(smb);
            bv.getInputView(3).setChildView(smbv);

            helper = new WorkspaceHelper(getActivity(), null, R.style.BlocklyTestTheme);

            Block dummyBlock2 = makeDummyBlock();
            dummyBlock2.setInputsInline(true);
            bv = helper.obtainBlockView(dummyBlock2);
            bg.addView(bv);

            smb = makeStatementBlock();
            smbv = helper.obtainBlockView(smb);
            bv.getInputView(3).setChildView(smbv);

            wv.addView(bg);

            helper = mWorkspace.getWorkspaceHelper();

            bg = new BlockGroup(getActivity(), helper);
            Block outerBlock = makeOuterBlock();
            outerBlock.setInputsInline(true);
            mWorkspace.addRootBlock(outerBlock);
            BlockView obv = helper.obtainBlockView(outerBlock);
            bg.addView(obv);
            wv.addView(bg);

            Block innerBlock = makeInnerBlock();
            BlockView ibv = helper.obtainBlockView(innerBlock);
            obv.getInputView(1).setChildView(ibv);

            Block ivb2 = makeValueInputBlock();
            BlockView ivv2 = helper.obtainBlockView(ivb2);
            ibv.getInputView(2).setChildView(ivv2);

//            airstrike(wv, 10);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
            mWorkspace = new Workspace();
        }

        private void airstrike(WorkspaceView wv, int numBlocks) {
            WorkspaceHelper helper = mWorkspace.getWorkspaceHelper();
            Block dummyBlock;
            BlockGroup bg;
            BlockView bv;
            for (int i = 0; i < numBlocks; i++) {
                dummyBlock = makeDummyBlock();
                int randomX = (int) (Math.random() * 250);
                int randomY = (int) (Math.random() * 500);
                dummyBlock.setPosition(randomX, randomY);

                mWorkspace.addRootBlock(dummyBlock);
                bg = new BlockGroup(getActivity(), helper);
                bv = helper.obtainBlockView(dummyBlock);
                bg.addView(bv);
                wv.addView(bg);
            }
        }

        private Block makeDummyBlock() {
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
            input.add(new Field.FieldLabel("DO", "this is a loop"));
            bob.addInput(input);

            input = new Input.InputValue("input7", Input.ALIGN_RIGHT, null);
            input.add(new Field.FieldColour("color", 0xFF0000));
            bob.addInput(input);

            input = new Input.InputValue("input8", null, null);
            input.add(new Field.FieldInput("input text", "initial wide field of text"));
            bob.addInput(input);

            input = new Input.InputStatement("input9", null, null);
            input.add(new Field.FieldLabel("DO", "another loop"));
            bob.addInput(input);

            bob.setColour(42);
            return bob.build();
        }

        private Block makeOuterBlock() {
            Block.Builder block = new Block.Builder("dummy");
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

        private Block makeInnerBlock() {
            Block.Builder block = new Block.Builder("dummy");
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

        private Block makeSimpleValueBlock() {
            Block.Builder block = new Block.Builder("dummy");
            block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

            Input input = new Input.InputDummy("input", null);
            input.add(new Field.FieldLabel("label", "zero"));
            block.addInput(input);

            block.setColour(210);
            return block.build();
        }

        private Block makeValueInputBlock() {
            Block.Builder block = new Block.Builder("dummy");
            block.setOutput(new Connection(Connection.CONNECTION_TYPE_OUTPUT, null));

            Input input = new Input.InputValue("input", null, null);
            input.add(new Field.FieldLabel("label", "one input"));
            block.addInput(input);

            block.setColour(110);
            return block.build();
        }

        private Block makeStatementBlock() {
            Block.Builder block = new Block.Builder("dummy");
            block.setPrevious(new Connection(Connection.CONNECTION_TYPE_PREVIOUS, null));
            block.setNext(new Connection(Connection.CONNECTION_TYPE_NEXT, null));

            Input input = new Input.InputDummy("input", null);
            input.add(new Field.FieldLabel("label", "do something"));
            block.addInput(input);

            block.setColour(240);
            return block.build();
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }
    }
}

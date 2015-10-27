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

package com.google.blockly.model;

import android.content.Context;

import com.google.blockly.ToolboxFragment;
import com.google.blockly.TrashFragment;
import com.google.blockly.control.BlockCopyBuffer;
import com.google.blockly.control.ConnectionManager;
import com.google.blockly.control.Dragger;
import com.google.blockly.control.ProcedureManager;
import com.google.blockly.control.WorkspaceStats;
import com.google.blockly.ui.BlockGroup;
import com.google.blockly.ui.WorkspaceHelper;
import com.google.blockly.ui.WorkspaceView;
import com.google.blockly.utils.BlocklyXmlHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Keeps track of all the global state used in the workspace. This is mostly just blocks.
 */
public class Workspace {
    private static final String TAG = "Workspace";
    private static final boolean DEBUG = true;

    private final ArrayList<Block> mRootBlocks = new ArrayList<>();
    private final ProcedureManager mProcedureManager = new ProcedureManager();
    private final NameManager mVariableNameManager = new NameManager.VariableNameManager();
    private final ConnectionManager mConnectionManager = new ConnectionManager();
    private final WorkspaceStats stats = new WorkspaceStats(mVariableNameManager, mProcedureManager,
            mConnectionManager);

    private ToolboxFragment mToolbox;
    private ArrayList<Block> mDeletedBlocks = new ArrayList<>();
    private TrashFragment mTrash;

    private WorkspaceHelper mWorkspaceHelper;
    private WorkspaceView mWorkspaceView;
    private final Dragger mDragger =
            new Dragger(mWorkspaceHelper, mWorkspaceView, mConnectionManager, mRootBlocks);

    public Workspace() {
        mToolbox.setWorkspace(this);
        mTrash.setWorkspace(this);
    }

    /**
     * Adds a new block to the workspace as a root block.
     *
     * @param block
     */
    public void addRootBlock(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("Cannot add a null block as a root block");
        }
        if (block.getPreviousBlock() != null) {
            throw new IllegalArgumentException("Root blocks may not have a previous block");
        }
        if (mRootBlocks.contains(block)) {
            throw new IllegalArgumentException("Block is already a root block.");
        }
        mRootBlocks.add(block);
        stats.collectStats(block, true);
    }

    public boolean removeRootBlock(Block block) {
        mDeletedBlocks.add(block);
       // mTrash.
        return mRootBlocks.remove(block);
    }

    public WorkspaceHelper getWorkspaceHelper() {
        return mWorkspaceHelper;
    }

    public void setWorkspaceHelper(WorkspaceHelper helper) {
        mWorkspaceHelper = helper;
        mDragger.setWorkspaceHelper(mWorkspaceHelper);
    }

    /**
     * Reads the workspace in from an XML string.
     *
     * @param is The input stream to read from.
     *
     * @throws BlocklyParserException
     */
    public void loadFromXml(InputStream is, BlockFactory blockFactory)
            throws BlocklyParserException {
        mRootBlocks.addAll(BlocklyXmlHelper.loadFromXml(is, blockFactory, stats));
        for (int i = 0; i < mRootBlocks.size(); i++) {
            stats.collectStats(mRootBlocks.get(i), true /* recursive */);
        }
    }

    /**
     * Outputs the workspace as an XML string.
     *
     * @param os The output stream to write to.
     *
     * @throws BlocklySerializerException
     */
    public void serializeToXml(OutputStream os) throws BlocklySerializerException {
        BlocklyXmlHelper.writeToXml(mRootBlocks, os);
    }

    /**
     * Recursively initialize views corresponding to every block in the model.
     *
     * @param wv The root workspace view to add to.
     * @param context The activity context.
     */
    public void createViewsFromModel(WorkspaceView wv, Context context) {
        BlockGroup bg;
        mWorkspaceView = wv;
        mDragger.setWorkspaceView(mWorkspaceView);
        mWorkspaceView.setDragger(mDragger);
        for (int i = 0; i < mRootBlocks.size(); i++) {
            bg = new BlockGroup(context, mWorkspaceHelper);
            mWorkspaceHelper.obtainBlockView(mRootBlocks.get(i), bg, mConnectionManager);
            mWorkspaceView.addView(bg);
        }
    }

    public void addRootBlockAndView(Block block, Context context) {
        BlockGroup bg = new BlockGroup(context, mWorkspaceHelper);
        mWorkspaceHelper.obtainBlockView(context, block, bg, mConnectionManager);
        mWorkspaceView.addView(bg);
        addRootBlock(block);
    }
}

package com.tiecode.plugin.guard.controlflow;

import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.TreeModifier;

public class ControlFlowConfuser extends TreeModifier<Void> {
    @Override
    public TCTree visitEvent(TCTree.TCEvent tree, Void param) {
        return tree;
    }

    @Override
    public TCTree visitMethod(TCTree.TCMethodDeclare tree, Void param) {
        return tree;
    }
}

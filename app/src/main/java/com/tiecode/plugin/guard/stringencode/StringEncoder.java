package com.tiecode.plugin.guard.stringencode;

import com.tiecode.platform.compiler.toolchain.parser.TiecodeToken;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.TreeMaker;
import com.tiecode.platform.compiler.toolchain.tree.TreeModifier;
import com.tiecode.platform.compiler.toolchain.util.Names;

import java.util.ArrayList;
import java.util.List;

public class StringEncoder extends TreeModifier<Void> {

    TreeMaker maker;

    public StringEncoder() {
        this.maker = new TreeMaker();
    }

    private String encode(String text) {
        int len = text.length();
        char[] chars = new char[len];
        for (int i = 0;i < len;i++) {
            char ch = (char) (text.charAt(i) - len);
            chars[i] = ch;
        }
        return new String(chars);
    }

    @Override
    public TCTree visitAnnotation(TCTree.TCAnnotation tree, Void param) {
        return tree;
    }

    @Override
    public TCTree visitCase(TCTree.TCCase tree, Void param) {
        return tree;
    }

    @Override
    public TCTree visitLiteral(TCTree.TCLiteral tree, Void param) {
        if (tree.token == TiecodeToken.STRING) {
            String value = (String) tree.value;
            tree.value = encode(value);
            List<TCTree.TCExpression> args = new ArrayList<>();
            args.add(tree);
            return maker.methodInvocation(Names.of("__解密"), null, args);
        }
        return tree;
    }

    @Override
    public TCTree visitMethodInvocation(TCTree.TCMethodInvocation tree, Void param) {
        if (tree.arguments != null) {
            for (int i = 0; i < tree.arguments.size(); i++) {
                tree.arguments.set(i, (TCTree.TCExpression) modify(tree.arguments.get(i)));
            }
        }
        return tree;
    }

    @Override
    public TCTree visitVariable(TCTree.TCVariableDeclare tree, Void param) {
        if (!tree.modifiers.isConst()) {
            tree.initializer = (TCTree.TCExpression) modify(tree.initializer);
        }
        return tree;
    }
}

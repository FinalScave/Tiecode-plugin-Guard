package com.tiecode.plugin.guard.stringencode;

import com.tiecode.platform.compiler.api.descriptor.Name;
import com.tiecode.platform.compiler.api.process.Filter;
import com.tiecode.platform.compiler.api.process.Trees;
import com.tiecode.platform.compiler.source.tree.ClassTree;
import com.tiecode.platform.compiler.source.tree.Tree;
import com.tiecode.platform.compiler.toolchain.env.Context;
import com.tiecode.platform.compiler.toolchain.parser.TiecodeToken;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.TreeMaker;
import com.tiecode.platform.compiler.toolchain.tree.TreeModifier;
import com.tiecode.platform.compiler.toolchain.tree.code.TiecodeTrees;
import com.tiecode.platform.compiler.toolchain.tree.symbol.Symbol;
import com.tiecode.platform.compiler.toolchain.tree.type.Type;
import com.tiecode.platform.compiler.toolchain.util.Names;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class StringEncoder extends TreeModifier<Void> {
    private final static String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private final TreeMaker maker;
    private final Context context;

    public StringEncoder(Context context) {
        this.maker = new TreeMaker();
        this.context = context;
    }

    private String toBinary(byte[] bytes) {
        StringBuilder builder = new StringBuilder(new BigInteger(1, bytes).toString(2));
        while (builder.length() % 8 != 0) {
            builder.insert(0, "0");
        }
        return builder.toString();
    }

    private String encode(String text) {
        String add = "=";
        byte[] bytes = text.getBytes();
        StringBuilder base64Str = new StringBuilder();
        String bytesBinary = toBinary(bytes);
        int addCount = 0;
        while (bytesBinary.length() % 24 != 0) {
            bytesBinary += "0";
            addCount++;
        }
        for (int i = 0; i <= bytesBinary.length() - 6; i += 6) {
            int index = Integer.parseInt(bytesBinary.substring(i, i + 6), 2);
            if (index == 0 && i >= bytesBinary.length() - addCount) {
                base64Str.append(add);
            } else {
                base64Str.append(CHARS.charAt(index));
            }
        }
        String s = base64Str.toString();
        s += (char) ('A' + (int) ((Math.random() * 10) + 10));
        s += (char) ('a' + (int) ((Math.random() * 10) + 10));
        return s;
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
            Trees trees = TiecodeTrees.instance(context);
            Name decode = trees.getName("__解密");
            TCTree.TCMethodInvocation invocation = maker.methodInvocation(decode, null, args);
            return invocation;
        }
        return tree;
    }

    @Override
    public TCTree visitVariable(TCTree.TCVariableDeclare tree, Void param) {
        Type type = tree.symbol.type;
        if (type.isClassType()) {
            Type.ClassType classType = (Type.ClassType) type;
            if (classType.symbol.constType != null) {
                return tree;
            }
        }
        if (!tree.modifiers.isConst()) {
            tree.initializer = modify(tree.initializer);
        }
        return tree;
    }
}

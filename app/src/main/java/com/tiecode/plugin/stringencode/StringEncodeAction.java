package com.tiecode.plugin.stringencode;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.develop.util.firstparty.android.AssetUtils;
import com.tiecode.platform.compiler.api.file.FileManager;
import com.tiecode.platform.compiler.api.file.TiecodeTempFile;
import com.tiecode.platform.compiler.api.process.TaskEvent;
import com.tiecode.platform.compiler.api.process.Trees;
import com.tiecode.platform.compiler.source.tree.CompilationUnitTree;
import com.tiecode.platform.compiler.toolchain.env.Context;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.code.TiecodeTrees;

import java.util.List;

public class StringEncodeAction extends TiecodeCompilerAction {
    private final static String ENCODE_SOURCE_NAME = "encode.t";
    private Context context;

    @Override
    public void init(Context context) {
        this.context = context;
    }

    @Override
    public void onEventStarted(TaskEvent event) {
        if (event.getKind() == TaskEvent.Kind.PARSE) {
            String content = AssetUtils.readAsset(StringEncodePlugin.pluginContext, ENCODE_SOURCE_NAME);
            FileManager manager = context.get(FileManager.key);
            manager.addSourceFile(new TiecodeTempFile(ENCODE_SOURCE_NAME, content));
        } else if (event.getKind() == TaskEvent.Kind.ENTER) {
            Trees trees = TiecodeTrees.instance(context);
            List<? extends CompilationUnitTree> units = trees.getCompilationUnits();
            for (CompilationUnitTree unit : units) {
                StringEncoder encoder = new StringEncoder();
                encoder.modify((TCTree.TCCompilationUnit) unit);
            }
        }
    }
}

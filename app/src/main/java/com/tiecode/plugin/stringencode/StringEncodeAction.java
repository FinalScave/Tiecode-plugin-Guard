package com.tiecode.plugin.stringencode;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.develop.util.firstparty.android.AssetUtils;
import com.tiecode.platform.compiler.api.file.FileManager;
import com.tiecode.platform.compiler.api.file.TiecodeTempFile;
import com.tiecode.platform.compiler.api.process.TaskEvent;
import com.tiecode.platform.compiler.api.process.Trees;
import com.tiecode.platform.compiler.source.tree.CompilationUnitTree;
import com.tiecode.platform.compiler.toolchain.env.Context;
import com.tiecode.platform.compiler.toolchain.env.Options;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.code.TiecodeTrees;

import java.util.List;

public class StringEncodeAction extends TiecodeCompilerAction {
    private Context context;
    private Options options;

    @Override
    public void init(Context context) {
        this.context = context;
        this.options = Options.instance(context);
    }

    @Override
    public void onEventStarted(TaskEvent event) {
        if (options.outPath == null) {
            return;
        }
        switch (event.getKind()) {
            case PARSE:
                writeEncodeSource();
                break;
            case ENTER:
                modifyStrings();
                break;
        }
    }

    private void writeEncodeSource() {
        String filename = "decode-android.t";
        Options.Target target = Options.instance(context).target;
        if (target == Options.Target.LINUX) {
            filename = "decode-linux.t";
        } else if (target == Options.Target.JS) {
            filename = "decode-js.t";
        }
        String content = AssetUtils.readAsset(StringEncodePlugin.pluginContext, filename);
        FileManager manager = context.get(FileManager.key);
        manager.addSourceFile(new TiecodeTempFile(filename, content));
    }

    private void modifyStrings() {
        Trees trees = TiecodeTrees.instance(context);
        List<? extends CompilationUnitTree> units = trees.getCompilationUnits();
        for (CompilationUnitTree unit : units) {
            StringEncoder encoder = new StringEncoder();
            encoder.modify((TCTree.TCCompilationUnit) unit);
        }
    }
}

package com.tiecode.plugin.guard.action;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.develop.util.constant.SystemPath;
import com.tiecode.develop.util.firstparty.android.AssetUtils;
import com.tiecode.develop.util.firstparty.android.SettingUtils;
import com.tiecode.develop.util.firstparty.file.FileUtils;
import com.tiecode.platform.compiler.api.file.FileManager;
import com.tiecode.platform.compiler.api.file.TiecodeTempFile;
import com.tiecode.platform.compiler.api.process.TaskEvent;
import com.tiecode.platform.compiler.api.process.Trees;
import com.tiecode.platform.compiler.source.tree.CompilationUnitTree;
import com.tiecode.platform.compiler.toolchain.completer.RoundEnter;
import com.tiecode.platform.compiler.toolchain.env.Context;
import com.tiecode.platform.compiler.toolchain.env.Options;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.TreePrinter;
import com.tiecode.platform.compiler.toolchain.tree.code.TiecodeTrees;
import com.tiecode.plugin.guard.GuardPlugin;
import com.tiecode.plugin.guard.controlflow.ControlFlowConfuser;
import com.tiecode.plugin.guard.stringencode.StringEncoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GuardCompilerAction extends TiecodeCompilerAction {
    private Context context;

    @Override
    public void init(Context context) {
        this.context = context;
    }

    @Override
    public void onEventStarted(TaskEvent event) {
        if (Options.instance(context).outPath == null) {
            return;
        }
        switch (event.getKind()) {
            case PARSE:
                if (SettingUtils.getBoolean(GuardSettingAction.KEY_STRING_ENCODE, true)) {
                    writeDecodeSource();
                }
                break;
            case ANALYZE:
                if (SettingUtils.getBoolean(GuardSettingAction.KEY_STRING_ENCODE, true)) {
                    modifyStrings();
                }
                if (SettingUtils.getBoolean(GuardSettingAction.KEY_FLOW_CONFUSE, true)) {
                    modifyControlFlows();
                }
                break;
        }
    }

    private void writeDecodeSource() {
        String filename = "decode-android.t";
        Options.Target target = Options.instance(context).target;
        if (target == Options.Target.LINUX) {
            filename = "decode-linux.t";
        } else if (target == Options.Target.JS) {
            filename = "decode-js.t";
        }
        String content = AssetUtils.readAsset(GuardPlugin.pluginContext, filename);
        FileManager manager = context.get(FileManager.key);
        manager.addSourceFile(new TiecodeTempFile(filename, content));
    }

    private void modifyStrings() {
        Trees trees = TiecodeTrees.instance(context);
        List<? extends CompilationUnitTree> units = trees.getCompilationUnits();
        List<TCTree.TCCompilationUnit> newUnits = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            TCTree.TCCompilationUnit root = (TCTree.TCCompilationUnit) unit;
            if (!root.symbol.name.contentEquals(Options.instance(context).namespace)) {
                continue;
            }
            StringEncoder encoder = new StringEncoder(context);
            encoder.modify(root);
            newUnits.add(root);
        }
        RoundEnter enter = RoundEnter.instance(context);
        enter.complete(newUnits);
    }

    private void modifyControlFlows() {
        Trees trees = TiecodeTrees.instance(context);
        List<? extends CompilationUnitTree> units = trees.getCompilationUnits();
        List<TCTree.TCCompilationUnit> newUnits = new ArrayList<>();
        for (CompilationUnitTree unit : units) {
            TCTree.TCCompilationUnit root = (TCTree.TCCompilationUnit) unit;
            if (!root.symbol.name.contentEquals(Options.instance(context).namespace)) {
                continue;
            }
            ControlFlowConfuser confuser = new ControlFlowConfuser(context);
            confuser.modify(root);
            newUnits.add(root);
            printNewUnit(root);
        }
        RoundEnter enter = RoundEnter.instance(context);
        enter.complete(newUnits);
    }

    private void printNewUnit(TCTree.TCCompilationUnit root) {
        TreePrinter printer = new TreePrinter();
        String newCode = printer.getString(root);
        File codeDir = new File(SystemPath.CACHE, "guard_tmp");
        if (!codeDir.exists()) {
            codeDir.mkdirs();
        }
        String name = root.fileObject.getName();
        try {
            FileUtils.writeFile(newCode, new File(codeDir, name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

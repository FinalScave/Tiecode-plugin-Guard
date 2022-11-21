package com.tiecode.plugin.guard;

import android.content.Context;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.plugin.action.ActionController;
import com.tiecode.plugin.action.page.code.CodeBodyPageAction;
import com.tiecode.plugin.action.page.setting.PluginSettingPageAction;
import com.tiecode.plugin.app.PluginApp;
import com.tiecode.plugin.guard.action.GuardCompilerAction;
import com.tiecode.plugin.guard.action.GuardSettingAction;

public final class GuardPlugin extends PluginApp {
    public static Context pluginContext;

    @Override
    public void onInitPlugin(Context context) {
        ActionController controller = new ActionController();
        controller.addAction(PluginSettingPageAction.class, new GuardSettingAction());
        controller.addAction(TiecodeCompilerAction.class, new GuardCompilerAction());
        setActionController(controller);
        pluginContext = this;
    }
}

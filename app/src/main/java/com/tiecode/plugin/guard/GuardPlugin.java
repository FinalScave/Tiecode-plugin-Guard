package com.tiecode.plugin.guard;

import android.content.Context;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.plugin.action.ActionController;
import com.tiecode.plugin.app.PluginApp;

public class GuardPlugin extends PluginApp {
    public static Context pluginContext;

    @Override
    public void onInitPlugin(Context context) {
        ActionController controller = new ActionController();
        controller.addAction(TiecodeCompilerAction.class, new GuardCompilerAction());
        setActionController(controller);
        pluginContext = this;
    }
}

package com.tiecode.plugin.stringencode;

import android.content.Context;

import com.tiecode.develop.plugin.chinese.base.api.compiler.TiecodeCompilerAction;
import com.tiecode.plugin.action.ActionController;
import com.tiecode.plugin.app.PluginApp;

public class StringEncodePlugin extends PluginApp {
    public static Context pluginContext;

    @Override
    public void onInitPlugin(Context context) {
        ActionController controller = new ActionController();
        controller.addAction(TiecodeCompilerAction.class, new StringEncodeAction());
        setActionController(controller);
        pluginContext = this;
    }
}

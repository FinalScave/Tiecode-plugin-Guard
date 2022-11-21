package com.tiecode.plugin.guard.action;

import com.tiecode.develop.component.api.item.ISettingItemView;
import com.tiecode.develop.util.firstparty.android.SettingUtils;
import com.tiecode.plugin.PluginEnvironment;
import com.tiecode.plugin.action.page.setting.PluginSettingPageAction;

public final class GuardSettingAction extends PluginSettingPageAction {
    public final static String KEY_STRING_ENCODE = "GUARD_STRING_ENCODE";
    public final static String KEY_FLOW_CONFUSE = "GUARD_FLOW_CONFUSE";

    @Override
    public void onCreate() {
        ISettingItemView item = addSettingItem("保护配置");
        boolean encodeString = SettingUtils.getBoolean(KEY_STRING_ENCODE, true);
        boolean flowConfuse = SettingUtils.getBoolean(KEY_FLOW_CONFUSE, true);
        item.addSwitchItem("字符串加密", "是否开启字符串加密", KEY_STRING_ENCODE,  encodeString);
        item.addSwitchItem("控制流混淆", "是否开启控制流混淆", KEY_FLOW_CONFUSE,  flowConfuse);
    }
}

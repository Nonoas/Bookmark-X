package indi.bookmarkx.listener;


import com.intellij.util.messages.Topic;

public interface SettingsListener {
    // 定义一个 Topic，名字可以随意，通常与接口同名
    Topic<SettingsListener> TOPIC = Topic.create("Bookmark Settings Changed", SettingsListener.class);

    // 当设置更新时调用的方法
    void settingsUpdated();
}
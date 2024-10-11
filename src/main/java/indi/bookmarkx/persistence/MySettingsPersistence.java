package indi.bookmarkx.persistence;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * TODO 类描述
 *
 * @author huangshengsheng
 * @date 2024/10/11 16:04
 */
@Service(Service.Level.APP)
@State(
        name = "MyPluginSettings",
        storages = {@Storage("MyPluginSettings.xml")}  // 应用级别存储
)
public final class MySettingsPersistence implements PersistentStateComponent<MySettingsPersistence> {
    public String someValue = "default";

    @Override
    public MySettingsPersistence getState() {
        return this;
    }

    @Override
    public void loadState(MySettingsPersistence state) {
        this.someValue = state.someValue;
    }

    public static MySettingsPersistence getInstance() {
        return ApplicationManager.getApplication().getService(MySettingsPersistence.class);
    }
}

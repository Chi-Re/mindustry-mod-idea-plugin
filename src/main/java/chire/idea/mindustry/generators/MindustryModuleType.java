package chire.idea.mindustry.generators;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MindustryModuleType extends ModuleType<MindustryProjectModuleBuilder> {

    private static final String ID = "MINDUSTRY_MOD_PROJECT";

    public MindustryModuleType() {
        super(ID);
    }

    @NotNull
    public static MindustryModuleType getInstance() {
        return (MindustryModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public @NotNull MindustryProjectModuleBuilder createModuleBuilder() {
        return new MindustryProjectModuleBuilder();
    }

    @Override
    public @NotNull String getName() {
        return "Mindustry Mod Project";
    }

    @Override
    public @NotNull String getDescription() {
        return "Generate a Gradle-based Mindustry mod project.";
    }

    @Override
    public @NotNull Icon getNodeIcon(boolean isOpened) {
        return IconLoader.getIcon("/icons/genericJavaProject.svg", getClass());
    }
}

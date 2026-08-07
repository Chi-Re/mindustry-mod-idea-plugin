package chire.idea.mindustry.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class MindustryRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, MindustryRunConfigurationOptions> {

    public MindustryRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull String name) {
        super(name, new RunConfigurationModule(project), factory);
    }

    @Override
    public @NotNull MindustryRunConfigurationOptions getOptions() {
        return (MindustryRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected @NotNull Class<? extends ModuleBasedConfigurationOptions> getDefaultOptionsClass() {
        return MindustryRunConfigurationOptions.class;
    }

    @Override
    public @NotNull Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @Override
    public @NotNull SettingsEditor<? extends MindustryRunConfiguration> getConfigurationEditor() {
        return new MindustryRunConfigurationEditor(getProject());
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new MindustryRunProfileState(environment, this);
    }
}

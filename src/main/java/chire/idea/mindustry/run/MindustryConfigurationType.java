package chire.idea.mindustry.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;

public class MindustryConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "MindustryRunConfiguration";
    private static final Icon ICON = IconLoader.getIcon("/icons/genericJavaProject.svg", MindustryConfigurationType.class);

    public MindustryConfigurationType() {
        super(ID, bundle("run.configuration.displayName"), bundle("run.configuration.description"), ICON);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public @NotNull String getId() {
                return ID;
            }

            @Override
            public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new MindustryRunConfiguration(project, this, bundle("run.configuration.name"));
            }

            @Override
            public @NotNull String getName() {
                return bundle("run.configuration.name");
            }

            @Override
            public @NotNull Icon getIcon() {
                return ICON;
            }
        });
    }

    public static MindustryConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(MindustryConfigurationType.class);
    }
}

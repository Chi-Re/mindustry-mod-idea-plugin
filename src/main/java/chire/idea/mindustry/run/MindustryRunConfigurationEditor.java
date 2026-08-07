package chire.idea.mindustry.run;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.HyperlinkEventAction;
import com.intellij.ui.dsl.builder.Row;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;
import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;

public class MindustryRunConfigurationEditor extends SettingsEditor<MindustryRunConfiguration> {
    private final Project project;
    private final JPanel panel;

    private final ModulesComboBox modulesComboBox = new ModulesComboBox();
    private final ConfigurationModuleSelector moduleSelector;
    private final JBTextField gradleTaskField = new JBTextField(15);
    private final JBTextField gameJarPathField = new JBTextField(30);
    private final JBTextField modsDirectoryField = new JBTextField(30);
    private final JBCheckBox buildBeforeRunBox = new JBCheckBox(bundle("run.buildBeforeRun"));
    private final JBCheckBox installModBox = new JBCheckBox(bundle("run.installMod"));

    public MindustryRunConfigurationEditor(Project project) {
        this.project = project;
        this.moduleSelector = new ConfigurationModuleSelector(project, modulesComboBox);
        this.panel = BuilderKt.panel(layout -> {
            layout.group("Mindustry", true, p -> {
                p.row(bundle("run.module"), r -> {
                    r.cell(modulesComboBox);
                    return Unit.INSTANCE;
                });
                p.row(bundle("run.gradleTask"), r -> {
                    r.cell(gradleTaskField);
                    return Unit.INSTANCE;
                });
                p.row(bundle("run.gameJarPath"), r -> {
                    r.cell(gameJarPathField);
                    rowComment(r, bundle("run.comment.gameJarPath"));
                    return Unit.INSTANCE;
                });
                p.row(bundle("run.modsDirectory"), r -> {
                    r.cell(modsDirectoryField);
                    rowComment(r, bundle("run.comment.modsDirectory"));
                    return Unit.INSTANCE;
                });
                p.row("", r -> {
                    r.cell(buildBeforeRunBox);
                    return Unit.INSTANCE;
                });
                p.row("", r -> {
                    r.cell(installModBox);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }

    private static void rowComment(Row row, String text) {
        row.rowComment(text, DEFAULT_COMMENT_WIDTH, HyperlinkEventAction.HTML_HYPERLINK_INSTANCE);
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }

    @Override
    protected void resetEditorFrom(@NotNull MindustryRunConfiguration configuration) {
        moduleSelector.reset(configuration);
        gradleTaskField.setText(configuration.getOptions().getGradleTask());
        gameJarPathField.setText(configuration.getOptions().getGameJarPath());
        modsDirectoryField.setText(configuration.getOptions().getModsDirectory());
        buildBeforeRunBox.setSelected(configuration.getOptions().isBuildBeforeRun());
        installModBox.setSelected(configuration.getOptions().isInstallMod());
    }

    @Override
    protected void applyEditorTo(@NotNull MindustryRunConfiguration configuration) {
        moduleSelector.applyTo(configuration);
        configuration.getOptions().setGradleTask(gradleTaskField.getText().trim());
        configuration.getOptions().setGameJarPath(gameJarPathField.getText().trim());
        configuration.getOptions().setModsDirectory(modsDirectoryField.getText().trim());
        configuration.getOptions().setBuildBeforeRun(buildBeforeRunBox.isSelected());
        configuration.getOptions().setInstallMod(installModBox.isSelected());
    }
}

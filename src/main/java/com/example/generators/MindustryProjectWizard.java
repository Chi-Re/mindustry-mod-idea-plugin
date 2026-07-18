package com.example.generators;

import com.intellij.ide.wizard.GeneratorNewProjectWizard;
import com.intellij.ide.wizard.NewProjectWizardChainStep;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.ide.wizard.RootNewProjectWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MindustryProjectWizard implements GeneratorNewProjectWizard {

    @Override
    public @NotNull String getId() {
        return "MindustryModProjectWizard";
    }

    @Override
    public @NotNull String getName() {
        return "Mindustry Mod Project";
    }

    @Override
    public @NotNull Icon getIcon() {
        return IconLoader.getIcon("/icons/genericJavaProject.svg", getClass());
    }

    @Override
    public @Nullable String getDescription() {
        return "Generate a Gradle-based Mindustry mod project.";
    }

    @Override
    public int getOrdinal() {
        return 50;
    }

    @Override
    public @NotNull NewProjectWizardStep createStep(@NotNull WizardContext context) {
        return null;
//        return new MindustryModuleStep(new RootNewProjectWizardStep(context));
    }
}

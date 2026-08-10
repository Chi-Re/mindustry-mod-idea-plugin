package chire.idea.mindustry.generators;

import chire.idea.mindustry.run.MindustryConfigurationType;
import chire.idea.mindustry.run.MindustryRunBundle;
import chire.idea.mindustry.run.MindustryRunConfiguration;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.starters.local.*;
import com.intellij.ide.starters.local.wizard.StarterInitialStep;
import com.intellij.ide.starters.shared.StarterLanguage;
import com.intellij.ide.starters.shared.StarterProjectType;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public class MindustryProjectModuleBuilder extends StarterModuleBuilder {
    public static final StarterLanguage JAVA = new StarterLanguage(
            "java",       // id
            "Java",       // title
            "JAVA",       // languageId (通常使用 Lang.java 的 id)
            true,         // isBuiltIn
            null          // description (可选)
    );

    public static final Key<MindustryProjectModel> PROJECT_MODEL_KEY = Key.create("mindustry.project.model");

    private final FileTemplateManager manager = FileTemplateManager.getInstance(ProjectManager.getInstance().getDefaultProject());

    private Path targetDir;

    @Override
    public @NotNull String getBuilderId() {
        return "MINDUSTRY_MODULE";
    }

    @Override
    public @Nullable Icon getNodeIcon() {
        return IconLoader.getIcon("/icons/genericJavaProject.svg", getClass());
    }

    @Override
    public @NotNull String getPresentableName() {
        return "Mindustry Mod Project";
    }

    @Override
    public @NotNull String getDescription() {
        return "";
    }

    @Override
    protected @NotNull List<StarterProjectType> getProjectTypes() {
        return List.of(
                new StarterProjectType("gradleGroovy", "Gradle Groovy DSL", "")
        );
    }

    @Override
    protected @NotNull List<StarterLanguage> getLanguages() {
        return List.of(JAVA);
    }

    @Override
    protected @NotNull List<GeneratorAsset> getAssets(@NotNull Starter starter) {
        List<String> assetsNames = List.of(
                "gradlew.bat",
                "gradlew",
                //这里保留src/XXX
                "gradle/wrapper/gradle-wrapper.jar",
                "gradle/wrapper/gradle-wrapper.properties",
                "assets/sprites/frog.png",
                "gradle.gitignore",
                ".github/workflows/commitTest.yml"
        );
//        List<String> templateNames = List.of(
//                "mod.hjson",  "build.gradle", "ExampleJavaMod.java.ft"
//        );

        Map<String, String> templateNames = new HashMap<>(){{
            put("mod.hjson", "mod.hjson");
            put("build.gradle", "build.gradle");
        }};

        MindustryProjectModel model = getStarterContext().getUserData(PROJECT_MODEL_KEY);
        if (model == null) {
            model = new MindustryProjectModel();
        }

        templateNames.put("ExampleJavaMod.java", "src/"+model.pluginCoordinates.main.replaceAll("\\.", "/")+".java");
//        model.buildSystemType.createBuildSystem(model).collectAssets { assets.add(it) }

        ArrayList<GeneratorAsset> assets = new ArrayList<>();

        for (String an : assetsNames) {
            assets.add(new GeneratorResourceFile(an, getDependencyConfig("/template/MindustryJavaModTemplate/"+an)));
        }

        for (String tn : templateNames.keySet()) {
//            assets.add(new GeneratorResourceFile(tn, getDependencyConfig("/template/MindustryJavaModTemplate/" + tn)));
            assets.add(new GeneratorTemplateFile(templateNames.get(tn), manager.getCodeTemplate(tn)));
        }

        return assets;
    }

    @Override
    public @NotNull Map<String, Object> getTemplateProperties() {
        MindustryProjectModel model = getStarterContext().getUserData(PROJECT_MODEL_KEY);
        if (model == null) {
            model = new MindustryProjectModel();
        }
        Map<String, Object> templateProperties = new HashMap<>();

        for (Field field : model.pluginCoordinates.getClass().getFields()) {
            if (field.getType() != String.class) continue;

            try {
                templateProperties.put("MINDUSTRY_"+field.getName().toUpperCase(), field.get(model.pluginCoordinates));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        for (Field field : model.getClass().getFields()) {
            if (field.getType() != String.class) continue;

            try {
                templateProperties.put("PLUGINS_"+field.getName().toUpperCase(), field.get(model));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return templateProperties;
    }

    @Override
    protected @NotNull StarterPack getStarterPack() {
        return new StarterPack(
                "mindustry",
                Collections.singletonList(
                        new Starter("mindustry", "Mindustry Console", getDependencyConfig("/starters/compose.pom"), Collections.emptyList())
                )
        );
    }

    @Override
    public @NotNull ModuleWizardStep[] createWizardSteps(@NotNull WizardContext context, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[0];
    }

    @Override
    protected void setupModule(@NotNull Module module) throws ConfigurationException {
        getStarterContext().setStarter(getStarterContext().starterPack.getStarters().getFirst());
        if (getStarterContext().getStarter() != null) {
            getStarterContext().setStarterDependencyConfig(loadDependencyConfig().get(getStarterContext().getStarter().getId()));
        }

        super.setupModule(module);

        installRunConfiguration(module);
    }

    private void installRunConfiguration(@NotNull Module module) {
        Project project = module.getProject();
        RunManager runManager = RunManager.getInstance(project);

        for (RunConfiguration existing : runManager.getAllConfigurationsList()) {
            if (existing instanceof MindustryRunConfiguration) {
                return;
            }
        }

        ConfigurationFactory factory = MindustryConfigurationType.getInstance().getConfigurationFactories()[0];
        MindustryRunConfiguration configuration =
                new MindustryRunConfiguration(project, factory, MindustryRunBundle.bundle("run.configuration.name"));
        configuration.getConfigurationModule().setModule(module);

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(configuration, factory);
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
    }

    @Override
    protected @NotNull StarterInitialStep createOptionsStep(@NotNull StarterContextProvider contextProvider) {
        return new MindustryModuleStep(contextProvider);
    }
}

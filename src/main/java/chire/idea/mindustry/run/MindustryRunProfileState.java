package chire.idea.mindustry.run;

import chire.idea.mindustry.settings.MindustrySettingsState;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;

public class MindustryRunProfileState extends JavaCommandLineState {
    private final MindustryRunConfiguration configuration;

    public MindustryRunProfileState(ExecutionEnvironment environment, MindustryRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
    }

    @Override
    protected @NotNull JavaParameters createJavaParameters() throws ExecutionException {
        Project project = getEnvironment().getProject();
        Module module = configuration.getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException(bundle("run.error.noModule"));
        }

        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) {
            sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        }
        if (sdk == null || sdk.getHomePath() == null) {
            throw new ExecutionException(bundle("run.error.noJdk"));
        }

        MindustryRunConfigurationOptions options = configuration.getOptions();
        String gamePath = options.getGameJarPath() != null && !options.getGameJarPath().isEmpty()
                ? options.getGameJarPath()
                : MindustrySettingsState.getInstance().gameJarPath;
        Path gameJar = Path.of(gamePath);
        if (!Files.isRegularFile(gameJar)) {
            throw new ExecutionException(bundle("run.error.noGameJar", gameJar));
        }

        JavaParameters params = new JavaParameters();
        params.setJdk(sdk);
        params.setJarPath(gameJar.toString());
        params.setWorkingDirectory(moduleDir(module).toFile());
        params.setModuleName(module.getName());

        if (DefaultDebugExecutor.EXECUTOR_ID.equals(getEnvironment().getExecutor().getId())) {
             params.getProgramParametersList().add("-debug");
        }
        return params;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        Project project = getEnvironment().getProject();
        MindustryRunConfigurationOptions options = configuration.getOptions();

        Module module = configuration.getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException(bundle("run.error.noModule"));
        }
        Path moduleDir = moduleDir(module);

        ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

        if (options.isBuildBeforeRun()) {
            MindustryGradleBuilder.runTask(project, moduleDir, options.getGradleTask(), console);
        }

        if (options.isInstallMod()) {
            installMod(console, module, moduleDir, options);
        }

        print(console, "\n" + bundle("run.text.launching", launchingText()) + "\n");

        OSProcessHandler gameHandler = super.startProcess();
        gameHandler.setShouldDestroyProcessRecursively(true);
        gameHandler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                Integer exitCode = gameHandler.getExitCode();

                if (exitCode != null && exitCode != 0) {
                    console.print(bundle("run.error.gameExited", exitCode) + "\n",
                            ConsoleViewContentType.ERROR_OUTPUT);
                }
            }
        });
        console.attachToProcess(gameHandler);
        return new DefaultExecutionResult(console, gameHandler);
    }

    private String launchingText() {
        try {
            return getJavaParameters().toCommandLine().getCommandLineString();
        } catch (Exception e) {
            return configuration.getName();
        }
    }

    private Path moduleDir(Module module) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        if (roots.length > 0) {
            return Path.of(roots[0].getPath());
        }
        String basePath = module.getProject().getBasePath();
        return basePath == null ? Path.of("") : Path.of(basePath);
    }

    private void installMod(ConsoleView console, Module module, Path moduleDir,
                            MindustryRunConfigurationOptions options) throws ExecutionException {
        Path modJar = findModJar(module, moduleDir);
        if (modJar == null) {
            throw new ExecutionException(bundle("run.error.noModJar", moduleDir.resolve("build/libs")));
        }
        String modsDirText = options.getModsDirectory() != null && !options.getModsDirectory().isEmpty()
                ? options.getModsDirectory()
                : MindustrySettingsState.getInstance().modsDirectory;
        Path modsDir = Path.of(modsDirText);
        try {
            Files.createDirectories(modsDir);
            Path target = modsDir.resolve(modJar.getFileName());
            // 涓?debugDesktop 浠诲姟涓€鑷达細鍏堝垹闄ゆ棫鏂囦欢锛屽啀澶嶅埗鏂?jar
            Files.deleteIfExists(target);
            Files.copy(modJar, target);
            print(console, bundle("run.text.installing", target) + "\n");
        } catch (IOException e) {
            throw new ExecutionException(bundle("run.error.cannotRun", e.getMessage()), e);
        }
    }

    private static @Nullable Path findModJar(Module module, Path moduleDir) {
        Path libs = moduleDir.resolve("build").resolve("libs");
        if (!Files.isDirectory(libs)) {
            return null;
        }
        // 涓?debugDesktop 浠诲姟涓€鑷达細浼樺厛绮剧‘鍖归厤 <妯″潡鍚?Desktop.jar
        Path named = libs.resolve(module.getName() + "Desktop.jar");
        if (Files.isRegularFile(named)) {
            return named;
        }
        try (Stream<Path> stream = Files.list(libs)) {
            List<Path> jars = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar") && Files.isRegularFile(p))
                    .toList();
            return jars.stream()
                    .filter(p -> p.getFileName().toString().endsWith("Desktop.jar"))
                    .findFirst()
                    .orElseGet(() -> jars.stream()
                            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                            .orElse(null));
        } catch (IOException e) {
            return null;
        }
    }

    private static void print(ConsoleView console, String text) {
        console.print(text, ConsoleViewContentType.SYSTEM_OUTPUT);
    }
}


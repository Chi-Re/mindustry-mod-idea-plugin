package chire.idea.mindustry.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.system.OS;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;

/**
 * 通过外部 Gradle wrapper 执行模组构建，输出流式写入控制台。
 */
public final class MindustryGradleBuilder {
    private static final long BUILD_TIMEOUT_MS = 30 * 60 * 1000L;

    private MindustryGradleBuilder() {
    }

    public static void runTask(@NotNull Project project, @NotNull Path projectDir,
                               @NotNull String task, @NotNull ConsoleView console) throws ExecutionException {
        print(console, bundle("run.text.compiling", task) + "\n");
        buildViaWrapper(projectDir, task, console);
        print(console, bundle("run.text.completed") + "\n");
    }

    private static void buildViaWrapper(Path projectDir, String task, ConsoleView console) throws ExecutionException {
        Path gradlew = projectDir.resolve(OS.CURRENT == OS.Windows ? "gradlew.bat" : "gradlew");
        if (!Files.isRegularFile(gradlew)) {
            throw new ExecutionException(bundle("run.error.noGradle", gradlew));
        }

        GeneralCommandLine commandLine = new GeneralCommandLine(gradlew.toString(), task)
                .withWorkDirectory(projectDir.toFile());

        ProcessHandler handler = new OSProcessHandler(commandLine);
        handler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                ConsoleViewContentType type = outputType == ProcessOutputTypes.STDOUT
                        ? ConsoleViewContentType.NORMAL_OUTPUT
                        : outputType == ProcessOutputTypes.STDERR
                        ? ConsoleViewContentType.ERROR_OUTPUT
                        : ConsoleViewContentType.SYSTEM_OUTPUT;
                console.print(event.getText(), type);
            }
        });
        handler.startNotify();

        long deadline = System.currentTimeMillis() + BUILD_TIMEOUT_MS;
        while (!handler.isProcessTerminated()) {
            if (System.currentTimeMillis() > deadline) {
                handler.destroyProcess();
                throw new ExecutionException(bundle("run.error.buildTimeout"));
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionException(e);
            }
        }

        Integer exitCode = handler.getExitCode();
        if (exitCode == null || exitCode != 0) {
            throw new ExecutionException(bundle("run.error.buildFailed", String.valueOf(exitCode)));
        }
    }

    private static void print(ConsoleView console, String text) {
        console.print(text, ConsoleViewContentType.SYSTEM_OUTPUT);
    }
}

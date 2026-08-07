package chire.idea.mindustry.run;

import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.util.xmlb.annotations.Tag;

public class MindustryRunConfigurationOptions extends ModuleBasedConfigurationOptions {
    private String gradleTask = "jar";
    private String gameJarPath = "";
    private String modsDirectory = "";
    private boolean buildBeforeRun = true;
    private boolean installMod = true;

    @Tag("gradleTask")
    public String getGradleTask() {
        return gradleTask;
    }

    public void setGradleTask(String gradleTask) {
        this.gradleTask = gradleTask;
    }

    @Tag("gameJarPath")
    public String getGameJarPath() {
        return gameJarPath;
    }

    public void setGameJarPath(String gameJarPath) {
        this.gameJarPath = gameJarPath;
    }

    @Tag("modsDirectory")
    public String getModsDirectory() {
        return modsDirectory;
    }

    public void setModsDirectory(String modsDirectory) {
        this.modsDirectory = modsDirectory;
    }

    @Tag("buildBeforeRun")
    public boolean isBuildBeforeRun() {
        return buildBeforeRun;
    }

    public void setBuildBeforeRun(boolean buildBeforeRun) {
        this.buildBeforeRun = buildBeforeRun;
    }

    @Tag("installMod")
    public boolean isInstallMod() {
        return installMod;
    }

    public void setInstallMod(boolean installMod) {
        this.installMod = installMod;
    }
}

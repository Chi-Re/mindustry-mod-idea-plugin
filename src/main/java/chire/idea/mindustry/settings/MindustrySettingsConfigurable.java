package chire.idea.mindustry.settings;

import chire.idea.mindustry.generators.MindustryVersion;
import chire.idea.mindustry.ui.MindustryVersionTablePanel;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.HyperlinkEventAction;
import com.intellij.ui.dsl.builder.Row;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.DefaultListModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;
import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;

public class MindustrySettingsConfigurable implements SearchableConfigurable {
    private final MindustrySettingsState state = MindustrySettingsState.getInstance();

    private JPanel panel;
    private boolean componentCreated;
    private JBTextField gameJarPathField;
    private JBTextField modsDirectoryField;
    private CheckBoxList<String> mirrorList;
    private final List<MindustrySettingsState.MirrorEntry> myMirrors = new ArrayList<>();
    private MindustryVersionTablePanel versionTablePanel;
    private String pendingVersion;

    @Override
    public @NotNull String getId() {
        return "settings.mindustry";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Mindustry";
    }

    @Override
    public @NotNull JComponent createComponent() {
        if (componentCreated) {
            return panel;
        }

        gameJarPathField = new JBTextField(30);
        modsDirectoryField = new JBTextField(30);
        mirrorList = new CheckBoxList<String>(this::mirrorToggle);
        versionTablePanel = MindustryVersionTablePanel.settingsMode(
                gameJarPathField::getText,
                this::onVersionDownloaded,
                this::currentMirrorUrl,
                this::currentMirrorPrefix);

        panel = BuilderKt.panel(layout -> {
            layout.group(bundle("settings.group.game"), true, p -> {
                p.row(bundle("settings.label.gameJarPath"), r -> {
                    r.cell(gameJarPathField);
                    r.cell(browseJarButton());
                    rowComment(r, bundle("settings.comment.gameJarPath"));
                    return Unit.INSTANCE;
                });
                p.row(bundle("settings.label.versionList"), r -> {
                    r.cell(versionTablePanel.createComponent());
                    return Unit.INSTANCE;
                }).resizableRow();
                return Unit.INSTANCE;
            });
            layout.group(bundle("settings.group.mirror"), true, p -> {
                p.row(bundle("settings.label.mirror"), r -> {
                    JBScrollPane scroll = new JBScrollPane(mirrorList);
                    scroll.setPreferredSize(new Dimension(380, 90));
                    r.cell(scroll);
                    return Unit.INSTANCE;
                });
                p.row("", r -> {
                    r.button(bundle("settings.button.addMirror"), e -> {
                        addMirror();
                        return Unit.INSTANCE;
                    });
                    r.button(bundle("settings.button.removeMirror"), e -> {
                        removeMirror();
                        return Unit.INSTANCE;
                    });
                    rowComment(r, bundle("settings.comment.mirror"));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
            layout.group(bundle("settings.group.mod"), true, p -> {
                p.row(bundle("settings.label.modsDirectory"), r -> {
                    r.cell(modsDirectoryField);
                    r.cell(browseDirButton());
                    rowComment(r, bundle("settings.comment.modsDirectory"));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        reset();

        componentCreated = true;

        return panel;
    }

    private JButton browseJarButton() {
        JButton button = new JButton(bundle("settings.button.browse"));
        button.addActionListener(e -> {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("jar");
            VirtualFile file = FileChooser.chooseFile(descriptor, null, existingFile(gameJarPathField.getText()));
            if (file != null) {
                gameJarPathField.setText(file.getPath());
            }
        });
        return button;
    }

    private JButton browseDirButton() {
        JButton button = new JButton(bundle("settings.button.browse"));
        button.addActionListener(e -> {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            VirtualFile file = FileChooser.chooseFile(descriptor, null, existingDir(modsDirectoryField.getText()));
            if (file != null) {
                modsDirectoryField.setText(file.getPath());
            }
        });
        return button;
    }

    private static @Nullable VirtualFile existingFile(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    private static @Nullable VirtualFile existingDir(String path) {
        return existingFile(path);
    }

    private static void rowComment(Row row, String text) {
        row.rowComment(text, DEFAULT_COMMENT_WIDTH, HyperlinkEventAction.HTML_HYPERLINK_INSTANCE);
    }

    private String currentMirrorUrl() {
        int index = mirrorList.getSelectedIndex();
        return index >= 0 ? mirrorList.getItemAt(index) : "";
    }

    private boolean currentMirrorPrefix() {
        String url = currentMirrorUrl();
        for (MindustrySettingsState.MirrorEntry entry : myMirrors) {
            if (url.equals(entry.url)) {
                return entry.prefixMode;
            }
        }
        return true;
    }

    private void mirrorToggle(int index, boolean value) {
        if (index < 0 || index >= myMirrors.size()) {
            return;
        }
        myMirrors.get(index).prefixMode = value;
    }

    private void addMirror() {
        String input = Messages.showInputDialog(
                (Project) null,
                bundle("settings.label.mirror"),
                bundle("settings.button.addMirror"),
                Messages.getQuestionIcon());
        if (input == null || input.isBlank()) {
            return;
        }
        String url = input.trim();
        if (!url.endsWith("/")) {
            url += "/";
        }
        for (MindustrySettingsState.MirrorEntry entry : myMirrors) {
            if (entry.url.equals(url)) {
                return;
            }
        }
        myMirrors.add(new MindustrySettingsState.MirrorEntry(url, true));
        mirrorList.addItem(url, url, true);
        mirrorList.setSelectedIndex(mirrorList.getModel().getSize() - 1);
    }

    private void removeMirror() {
        int index = mirrorList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        String url = mirrorList.getItemAt(index);
        myMirrors.removeIf(entry -> entry.url.equals(url));
        ((DefaultListModel<?>) mirrorList.getModel()).remove(index);
        if (mirrorList.getModel().getSize() > 0) {
            mirrorList.setSelectedIndex(Math.max(0, index - 1));
        }
    }

    private void onVersionDownloaded(String version, Path target) {
        gameJarPathField.setText(target.toString());
        pendingVersion = version;
    }

    @Override
    public boolean isModified() {
        if (!gameJarPathField.getText().equals(state.gameJarPath)) {
            return true;
        }
        if (!modsDirectoryField.getText().equals(state.modsDirectory)) {
            return true;
        }
        if (!String.valueOf(currentMirrorUrl()).equals(state.mirrorUrl)) {
            return true;
        }
        MindustryVersion.MindustryVersionKind kind = versionTablePanel.getKind();
        if (kind != null && !kind.name().equals(state.versionKind)) {
            return true;
        }
        if (pendingVersion != null && !pendingVersion.equals(state.gameVersion)) {
            return true;
        }
        return mirrorsDiffer();
    }

    private boolean mirrorsDiffer() {
        if (myMirrors.size() != state.mirrors.size()) {
            return true;
        }
        for (int i = 0; i < myMirrors.size(); i++) {
            MindustrySettingsState.MirrorEntry a = myMirrors.get(i);
            MindustrySettingsState.MirrorEntry b = state.mirrors.get(i);
            if (!a.url.equals(b.url) || a.prefixMode != b.prefixMode) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() {
        state.gameJarPath = gameJarPathField.getText().trim();
        state.modsDirectory = modsDirectoryField.getText().trim();
        MindustryVersion.MindustryVersionKind kind = versionTablePanel.getKind();
        if (kind != null) {
            state.versionKind = kind.name();
        }
        if (pendingVersion != null) {
            state.gameVersion = pendingVersion;
        }
        state.mirrorUrl = currentMirrorUrl();
        state.mirrors = new ArrayList<>();
        for (MindustrySettingsState.MirrorEntry entry : myMirrors) {
            state.mirrors.add(new MindustrySettingsState.MirrorEntry(entry.url, entry.prefixMode));
        }
        pendingVersion = null;
    }

    @Override
    public void reset() {
        gameJarPathField.setText(state.gameJarPath);
        modsDirectoryField.setText(state.modsDirectory);
        myMirrors.clear();
        for (MindustrySettingsState.MirrorEntry entry : state.mirrors) {
            myMirrors.add(new MindustrySettingsState.MirrorEntry(entry.url, entry.prefixMode));
        }
        mirrorList.clear();
        for (MindustrySettingsState.MirrorEntry entry : myMirrors) {
            mirrorList.addItem(entry.url, entry.url, entry.prefixMode);
        }
        int selected = -1;
        for (int i = 0; i < myMirrors.size(); i++) {
            if (myMirrors.get(i).url.equals(state.mirrorUrl)) {
                selected = i;
                break;
            }
        }
        if (selected >= 0) {
            mirrorList.setSelectedIndex(selected);
        }
        pendingVersion = null;
        versionTablePanel.reset(state.versionKind);
    }
}

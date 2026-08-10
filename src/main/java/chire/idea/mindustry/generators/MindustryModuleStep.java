package chire.idea.mindustry.generators;

import com.intellij.ide.starters.local.StarterContextProvider;
import com.intellij.ide.starters.local.wizard.StarterInitialStep;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.HyperlinkEventAction;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.Row;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.ui.UIUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static chire.idea.mindustry.generators.MindustryProjectModuleBuilder.PROJECT_MODEL_KEY;
import static chire.idea.mindustry.generators.MindustryProjectWizardBundle.bundle;
import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;
import static org.jetbrains.concurrency.Promises.runAsync;

public class MindustryModuleStep extends StarterInitialStep {
    private final MindustryProjectModel model = new MindustryProjectModel();

    private final GraphProperty<MindustryVersion.MindustryVersionKind> mindustryVersionKindGraphProperty = getPropertyGraph().property(MindustryVersion.MindustryVersionKind.Stable);

    private final GraphProperty<String> mindustryVersionGraphProperty = getPropertyGraph().property("");

    private final GraphProperty<String> mainProperty = getPropertyGraph().property("");

    private final GraphProperty<Integer> mindustryVersionPage = getPropertyGraph().property(1);

    private final GraphProperty<Boolean> versionSynchronous = getPropertyGraph().property(false);

    private Cell<ComboBox<String>> gameVersionBoxCell;
    private Cell<ComboBox<String>> minGameVersionBoxCell;
    private JButton pageMinusButton;
    private JButton pagePlusButton;
    private JLabel pageLabel;
    private boolean hasNextPage = true;

    public MindustryModuleStep(@NotNull StarterContextProvider parent) {
        super(parent);
    }

    @Override
    public void updateDataModel() {
        super.updateDataModel();

        getStarterContext().putUserData(
                PROJECT_MODEL_KEY,
                model
        );
    }

    @Override
    protected void addFieldsAfter(@NotNull Panel layout) {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(bundle.getMessage("tab.mod.information"), buildModInfoPanel());
        tabs.addTab(bundle.getMessage("tab.game.version"), buildGameVersionPanel());

        layout.row("", r -> {
            r.cell(tabs).resizableColumn();
            r.resizableRow();
            return Unit.INSTANCE;
        }).resizableRow();

        //触发创建
        //镜像可能已变更，清除版本缓存确保首次加载与当前设置一致
        MindustryVersion.MindustryVersionKind.clearAllCache();
        mindustryVersionKindGraphProperty.set(MindustryVersion.MindustryVersionKind.Stable);

        mainProperty.set(buildMainName());
    }

    private String buildMainName() {
        String group = getGroupIdProperty().get();
        String clazz = classNameFrom(getArtifactIdProperty().get());
        return group == null || group.isBlank() ? clazz : group + "." + clazz;
    }

    private static String classNameFrom(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return "ExampleJavaMod";
        }

        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;

        for (char c : artifactId.toCharArray()) {
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            } else {
                upperNext = true;
            }
        }

        String name = sb.toString();
        if (name.isEmpty()) {
            return "ExampleJavaMod";
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            name = "M" + name;
        }
        return name;
    }

    private JPanel buildModInfoPanel() {
        return BuilderKt.panel(panel -> {
            panel.row(bundle.getMessage("label.plugin.displayName"), row -> {
                var field = modelTextField(row.textField(), () -> "displayName").focused().getComponent();

                field.setColumns(25);

                rowComment(row, bundle.getMessage("comment.mindustry.displayName"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.name"), row -> {
                var field = modelTextField(row.textField(), () -> "name").focused().getComponent();

                field.setColumns(25);

                rowComment(row, bundle.getMessage("comment.mindustry.name"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.author"), row -> {
                var field = modelTextField(row.textField(), () -> "author").focused().getComponent();

                rowComment(row, bundle.getMessage("comment.mindustry.author"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.description"), row -> {
                var field = modelTextField(row.expandableTextField(
                        ParametersListUtil.DEFAULT_LINE_PARSER,
                        ParametersListUtil.DEFAULT_LINE_JOINER
                ), () -> "description").focused().getComponent();

                rowComment(row, bundle.getMessage("comment.mindustry.description"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.main"), row -> {
                var field = row.textField().onChanged((jbTextField) -> {
                    mainProperty.set(jbTextField.getText());

                    return Unit.INSTANCE;
                }).focused().getComponent();

                field.setColumns(25);

                rowComment(row, bundle.getMessage("comment.mindustry.main"));

                getGroupIdProperty().afterChange(string -> {
                    mainProperty.set(buildMainName());

                    return Unit.INSTANCE;
                });

                getArtifactIdProperty().afterChange(string -> {
                    mainProperty.set(buildMainName());

                    return Unit.INSTANCE;
                });

                mainProperty.afterChange(string -> {
                    model.pluginCoordinates.main = string;

                    model.mainClassName = getGroupIdProperty().get();
                    model.packageClassName = classNameFrom(getArtifactIdProperty().get());

                    field.setText(string);

                    return Unit.INSTANCE;
                });

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.version"), row -> {
                var field = modelTextField(row.textField(), () -> "version").focused().getComponent();

                field.setColumns(25);

                rowComment(row, bundle.getMessage("comment.mindustry.version"));

                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });
    }

    private JPanel buildGameVersionPanel() {
        return BuilderKt.panel(panel -> {
            panel.row(bundle.getMessage("label.version.kind"), row -> {
                row.segmentedButton(Arrays.stream(MindustryVersion.MindustryVersionKind.values()).toList(), (itemPresentation, string) -> {
                    switch (string) {
                        case Build -> itemPresentation.setText(
                                bundle.getMessage("label.version.build")
                        );
                        case Stable -> itemPresentation.setText(
                                bundle.getMessage("label.version.stable")
                        );
                    }

                    return Unit.INSTANCE;
                }).bind(mindustryVersionKindGraphProperty);

                rowComment(row, bundle.getMessage("comment.version.kind"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.gameVersion"), row -> {
                gameVersionBoxCell = row.comboBox(List.of("None"), null).enabled(false).onChanged(objectComboBox -> {
                    String value = objectComboBox.getItem() == null ? "" : objectComboBox.getItem().toString();

                    if (isValidVersion(value)) {
                        mindustryVersionGraphProperty.set(value);
                    }

                    return Unit.INSTANCE;
                });

                pageMinusButton = new JButton("-");
                pageLabel = new JLabel("1", SwingConstants.CENTER);
                pagePlusButton = new JButton("+");

                pageMinusButton.setPreferredSize(new Dimension(28, 28));
                pagePlusButton.setPreferredSize(new Dimension(28, 28));
                pageLabel.setPreferredSize(new Dimension(40, 28));

                pageMinusButton.addActionListener(e -> {
                    if (mindustryVersionPage.get() > 1) {
                        mindustryVersionPage.set(mindustryVersionPage.get() - 1);
                    }
                });

                pagePlusButton.addActionListener(e -> {
                    if (hasNextPage) {
                        mindustryVersionPage.set(mindustryVersionPage.get() + 1);
                    }
                });

                row.cell(pageMinusButton);
                row.cell(pageLabel);
                row.cell(pagePlusButton);

                rowComment(row, bundle.getMessage("comment.mindustry.gameVersion"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.minGameVersion"), row -> {
                minGameVersionBoxCell = row.comboBox(List.of("None"), null).enabled(false).onChanged(objectComboBox -> {
                    String value = objectComboBox.getItem() == null ? "" : objectComboBox.getItem().toString();

                    if (isValidVersion(value)) {
                        model.pluginCoordinates.minGameVersion = stripVersionPrefix(value);
                    }

                    return Unit.INSTANCE;
                });

                row.checkBox(bundle.getMessage("text.version.synchronous")).onChanged(jbCheckBox -> {
                    boolean checked = jbCheckBox.isSelected();

                    versionSynchronous.set(checked);

                    if (checked) {
                        String current = mindustryVersionGraphProperty.get();

                        if (current != null && !current.isEmpty()) {
                            minGameVersionBoxCell.getComponent().setItem(current);
                            model.pluginCoordinates.minGameVersion = stripVersionPrefix(current);
                        }
                    }

                    return Unit.INSTANCE;
                });

                rowComment(row, bundle.getMessage("comment.mindustry.minGameVersion"));

                return Unit.INSTANCE;
            });

            mindustryVersionGraphProperty.afterChange(string -> {
                if (string != null && !string.isEmpty()) {
                    model.pluginCoordinates.mindustryVersion = string;

                    if (versionSynchronous.get()) {
                        minGameVersionBoxCell.getComponent().setItem(string);
                        model.pluginCoordinates.minGameVersion = stripVersionPrefix(string);
                    }
                }

                return Unit.INSTANCE;
            });

            mindustryVersionKindGraphProperty.afterChange(arg -> {
                mindustryVersionPage.set(1);

                if (!versionSynchronous.get()) {
                    updateVersionItems(minGameVersionBoxCell, arg, 1, null);
                }

                return Unit.INSTANCE;
            });

            mindustryVersionPage.afterChange(page -> {
                loadGameVersions(mindustryVersionKindGraphProperty.get(), page);

                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });
    }

    private boolean isValidVersion(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if ("None".equals(value)) {
            return false;
        }
        if (bundle.getMessage("label.mirai.version.loading").equals(value)) {
            return false;
        }
        return !bundle.getMessage("label.mirai.version.loadFailed").equals(value);
    }

    private static String stripVersionPrefix(String version) {
        if (version != null && version.length() > 1
                && version.charAt(0) == 'v' && Character.isDigit(version.charAt(1))) {
            return version.substring(1);
        }
        return version;
    }

    private void loadGameVersions(MindustryVersion.MindustryVersionKind kind, int page) {
        pageMinusButton.setEnabled(false);
        pagePlusButton.setEnabled(false);

        updateVersionItems(gameVersionBoxCell, kind, page, versions -> {
            hasNextPage = versions == null || versions.size() >= MindustryVersion.PAGE_SIZE;
            updatePageControls();
        });
    }

    private void updatePageControls() {
        int page = mindustryVersionPage.get();

        pageLabel.setText(String.valueOf(page));
        pageMinusButton.setEnabled(page > 1);
        pagePlusButton.setEnabled(hasNextPage);
    }

    private <T extends JBTextField> Cell<T> modelTextField(Cell<T> row, ModelBox box) {
        Cell<T> cell = row.onChanged((jbTextField) -> {
            box.set(model.pluginCoordinates, jbTextField.getText());

            return Unit.INSTANCE;
        });

        cell.getComponent().setText(box.get(model.pluginCoordinates).toString());

        return cell;
    }

    private void rowComment(Row row, String string) {
        row.rowComment(string, DEFAULT_COMMENT_WIDTH, HyperlinkEventAction.HTML_HYPERLINK_INSTANCE);
    }

    public @NotNull Promise<List<String>> updateVersionItems(Cell<ComboBox<String>> versionCell,
                                                             MindustryVersion.MindustryVersionKind kind,
                                                             int page,
                                                             @Nullable Consumer<List<String>> onDone) {
        versionCell.getComponent().setEnabled(false);

        versionCell.enabled(false);

        versionCell.getComponent().removeAllItems();
        versionCell.getComponent().addItem(bundle.getMessage("label.mirai.version.loading"));

        return runAsync(() -> kind.getVersions(page)).onProcessed(versions -> {
            UIUtil.invokeLaterIfNeeded(() -> {
                versionCell.getComponent().removeAllItems();
                versions.forEach(k -> versionCell.getComponent().addItem(k));
                versionCell.getComponent().setEditable(true);
                versionCell.enabled(true);

                if (onDone != null) {
                    onDone.accept(versions);
                }
            });
        }).onError(error -> {
            UIUtil.invokeLaterIfNeeded(() -> {
                versionCell.getComponent().removeAllItems();
                versionCell.getComponent().addItem(bundle.getMessage("label.mirai.version.loadFailed"));
                versionCell.getComponent().setEditable(true);
                versionCell.enabled(true);

                if (onDone != null) {
                    onDone.accept(null);
                }
            });
        });
    }

    private interface ModelBox {
        String name();

        default void set(Object obj, String context) {
            try {
                obj.getClass().getField(name()).set(obj, context);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        default Object get(Object obj) {
            try {
                return obj.getClass().getField(name()).get(obj);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

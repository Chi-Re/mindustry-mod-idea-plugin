package com.example.generators;

import com.intellij.ide.starters.local.StarterContextProvider;
import com.intellij.ide.starters.local.wizard.StarterInitialStep;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.HyperlinkEventAction;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.Row;
import com.intellij.util.execution.ParametersListUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import java.util.Arrays;
import java.util.List;

import static com.example.generators.MindustryProjectModuleBuilder.PROJECT_MODEL_KEY;
import static com.example.generators.MindustryProjectWizardBundle.bundle;
import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;
import static org.jetbrains.concurrency.Promises.runAsync;

public class MindustryModuleStep extends StarterInitialStep {
    private final MindustryProjectModel model = new MindustryProjectModel();

    private final GraphProperty<MindustryVersion.MindustryVersionKind> mindustryVersionKindGraphProperty = getPropertyGraph().property(MindustryVersion.MindustryVersionKind.Stable);

    private final GraphProperty<String> mindustryVersionGraphProperty = getPropertyGraph().property("");

    private final GraphProperty<String> mainProperty = getPropertyGraph().property("");

    private final GraphProperty<Integer> mindustryVersionPage = getPropertyGraph().property(1);

    private final GraphProperty<Boolean> versionSynchronous = getPropertyGraph().property(false);

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
        layout.group(bundle.getMessage("title.plugin.information"), true, panel -> {
            panel.row(bundle.getMessage("label.plugin.displayName"), row -> {
//                var field = row.modelTextField().onChanged((jbTextField) -> {
//                    model.pluginCoordinates.displayName = jbTextField.getText();
//
//                    return Unit.INSTANCE;
//                }).focused().getComponent();

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
                    mainProperty.set(string+"."+getArtifactIdProperty().get());

                    return Unit.INSTANCE;
                });

                getArtifactIdProperty().afterChange(string -> {
                    mainProperty.set(getGroupIdProperty().get()+"."+string);

                    return Unit.INSTANCE;
                });

                mainProperty.afterChange(string -> {
                    model.pluginCoordinates.main = string;

                    model.mainClassName = getGroupIdProperty().get();
                    model.packageClassName = getArtifactIdProperty().get();

                    field.setText(string);

                    return Unit.INSTANCE;
                });

//                getGroupIdProperty().afterChange(string -> {
//
//                    return Unit.INSTANCE;
//                });

                return Unit.INSTANCE;
            });

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

                //TODO 旧版本游戏可能存在问题，比如Arc版本不匹配。
//                row.intTextField(new IntRange(1, 1000), );

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.version"), row -> {
                var box = row.comboBox(List.of("None"), null).enabled(false).onChanged(objectComboBox -> {
                    mindustryVersionGraphProperty.set(objectComboBox.getItem().toString());

                    return Unit.INSTANCE;
                });

                mindustryVersionKindGraphProperty.afterChange(arg -> {
                    updateVersionItems(box, arg);

                    return Unit.INSTANCE;
                });

                rowComment(row, bundle.getMessage("comment.mindustry.version"));

                return Unit.INSTANCE;
            });

            panel.row(bundle.getMessage("label.plugin.minGameVersion"), row -> {
                var box = row.comboBox(List.of("None"), null).enabled(false).onChanged(objectComboBox -> {
                    model.pluginCoordinates.minGameVersion = objectComboBox.getItem().toString();

                    return Unit.INSTANCE;
                });

                mindustryVersionGraphProperty.afterChange(string -> {
                    model.pluginCoordinates.version = string;

                    if (versionSynchronous.get()) {
                        box.getComponent().setItem(string);
                    }

                    return Unit.INSTANCE;
                });

                mindustryVersionKindGraphProperty.afterChange(arg -> {
                    if (!versionSynchronous.get()) {
                        updateVersionItems(box, arg);
                    }

                    return Unit.INSTANCE;
                });

                row.checkBox(bundle.getMessage("text.version.synchronous")).onChanged(jbCheckBox -> {
                    versionSynchronous.set(jbCheckBox.isSelected());

                    return Unit.INSTANCE;
                });

                rowComment(row, bundle.getMessage("comment.mindustry.minGameVersion"));

                return Unit.INSTANCE;
            });

            //触发创建
            mindustryVersionKindGraphProperty.set(MindustryVersion.MindustryVersionKind.Stable);

            return Unit.INSTANCE;
        });

        mainProperty.set(getGroupIdProperty().get()+"."+getArtifactIdProperty().get());
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

    public @NotNull Promise<List<String>> updateVersionItems(Cell<ComboBox<String>> versionCell, MindustryVersion.MindustryVersionKind kind){
        versionCell.getComponent().setEnabled(false);

        versionCell.enabled(false);

        versionCell.getComponent().removeAllItems();
        versionCell.getComponent().addItem(bundle.getMessage("label.mirai.version.loading"));

        return runAsync(() -> {
            var list = kind.getVersions();

            versionCell.getComponent().removeAllItems();

            list.forEach(k -> {
                versionCell.getComponent().addItem(k);
            });

            return list;
        }).onProcessed(versions -> {
            versionCell.getComponent().setEditable(true);
            versionCell.enabled(true);
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

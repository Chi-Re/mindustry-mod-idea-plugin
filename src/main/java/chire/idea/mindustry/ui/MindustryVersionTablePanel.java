package chire.idea.mindustry.ui;

import chire.idea.mindustry.generators.MindustryVersion;
import chire.idea.mindustry.settings.MindustryGameDownloader;
import chire.idea.mindustry.settings.MindustrySettingsState;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.table.JBTable;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static chire.idea.mindustry.run.MindustryRunBundle.bundle;

public class MindustryVersionTablePanel {
    private static final int ACTION_COLUMN = 2;

    private final @Nullable Supplier<String> gameJarPathSupplier;
    private final @Nullable BiConsumer<String, Path> onDownloaded;
    private final @Nullable Supplier<String> mirrorUrlSupplier;
    private final @Nullable BooleanSupplier mirrorPrefixSupplier;

    private final ComboBox<MindustryVersion.MindustryVersionKind> kindCombo =
            new ComboBox<>(new DefaultComboBoxModel<>(MindustryVersion.MindustryVersionKind.values()));
    private final JButton refreshButton = new JButton(bundle("settings.button.refresh"));
    private final JButton prevPageButton = new JButton(bundle("settings.button.previousPage"));
    private final JButton nextPageButton = new JButton(bundle("settings.button.nextPage"));
    private final JLabel pageLabel = new JLabel();
    private final JBLabel statusLabel = new JBLabel();
    private final VersionTableModel model;
    private final JBTable table;
    private final List<VersionRow> rows = new ArrayList<>();
    private int page = 1;
    private boolean hasNext = true;
    private JComponent component;

    private MindustryVersionTablePanel(@Nullable Supplier<String> gameJarPathSupplier,
                                       @Nullable BiConsumer<String, Path> onDownloaded,
                                       @Nullable Supplier<String> mirrorUrlSupplier,
                                       @Nullable BooleanSupplier mirrorPrefixSupplier) {
        this.model = new VersionTableModel();
        this.table = new JBTable(model);
        this.gameJarPathSupplier = gameJarPathSupplier;
        this.onDownloaded = onDownloaded;
        this.mirrorUrlSupplier = mirrorUrlSupplier;
        this.mirrorPrefixSupplier = mirrorPrefixSupplier;
    }

    public static MindustryVersionTablePanel settingsMode(@NotNull Supplier<String> gameJarPath,
                                                          @NotNull BiConsumer<String, Path> onDownloaded,
                                                          @NotNull Supplier<String> mirrorUrl,
                                                          @NotNull BooleanSupplier mirrorPrefix) {
        return new MindustryVersionTablePanel(gameJarPath, onDownloaded, mirrorUrl, mirrorPrefix);
    }

    public @NotNull JComponent createComponent() {
        if (component != null) {
            return component;
        }

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(26);
        table.putClientProperty("terminateEditOnFocusLost", true);

        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(0).setMaxWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(ACTION_COLUMN).setMaxWidth(120);

        table.getColumnModel().getColumn(1).setCellRenderer(new UrlCellRenderer());
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionCellRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTableClick(e);
            }
        });

        kindCombo.addActionListener(e -> onKindChanged());
        refreshButton.addActionListener(e -> loadPage(true));

        component = BuilderKt.panel(layout -> {
            layout.row("", r -> {
                r.label(bundle("settings.label.versionKind"));
                r.cell(kindCombo);
                r.cell(refreshButton);
                return Unit.INSTANCE;
            });
            layout.row("", r -> {
                r.cell(new JBScrollPane(table)).resizableColumn();
                r.resizableRow();
                return Unit.INSTANCE;
            }).resizableRow();
            layout.row("", r -> {
                r.cell(prevPageButton);
                r.cell(pageLabel);
                r.cell(nextPageButton);
                return Unit.INSTANCE;
            });
            layout.row("", r -> {
                r.cell(statusLabel);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

        prevPageButton.addActionListener(e -> {
            if (page > 1) {
                page--;
                loadPage(false);
            }
        });
        nextPageButton.addActionListener(e -> {
            page++;
            loadPage(false);
        });

        return component;
    }

    public void reset(@NotNull String kindName) {
        MindustryVersion.MindustryVersionKind target = kindOf(kindName);
        if ((MindustryVersion.MindustryVersionKind) kindCombo.getSelectedItem() != target) {
            kindCombo.setSelectedItem(target);
        } else if (rows.isEmpty()) {
            // 类型相同且尚未加载（首次打开）时加载第 1 页；
            // 已有数据则保持不变，避免设置框架重复 reset 导致页面闪动
            page = 1;
            loadPage(true);
        }
    }

    public @Nullable MindustryVersion.MindustryVersionKind getKind() {
        return (MindustryVersion.MindustryVersionKind) kindCombo.getSelectedItem();
    }

    private static MindustryVersion.MindustryVersionKind kindOf(String name) {
        for (MindustryVersion.MindustryVersionKind kind : MindustryVersion.MindustryVersionKind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return MindustryVersion.MindustryVersionKind.Stable;
    }

    private void onKindChanged() {
        page = 1;
        rows.clear();
        model.fireTableDataChanged();
        loadPage(true);
    }

    private String currentMirrorUrl() {
        if (mirrorUrlSupplier != null) {
            return mirrorUrlSupplier.get();
        }
        return MindustrySettingsState.currentMirrorUrl();
    }

    private boolean currentMirrorPrefix() {
        if (mirrorPrefixSupplier != null) {
            return mirrorPrefixSupplier.getAsBoolean();
        }
        return MindustrySettingsState.currentMirrorPrefix();
    }

    private void loadPage(boolean force) {
        MindustryVersion.MindustryVersionKind kind = (MindustryVersion.MindustryVersionKind) kindCombo.getSelectedItem();
        if (kind == null) {
            return;
        }
        String mirrorUrl = currentMirrorUrl();
        boolean prefix = currentMirrorPrefix();
        setLoading(true);

        Promises.runAsync(() -> kind.getVersions(page, force, mirrorUrl, prefix))
                .onSuccess(versions -> applyVersions(kind, versions, mirrorUrl, prefix))
                .onError(error -> {
                    setLoading(false);
                    statusLabel.setText(bundle("settings.text.loadFailed", error.getMessage()));
                });
    }

    private void applyVersions(MindustryVersion.MindustryVersionKind kind, List<String> versions,
                               String mirrorUrl, boolean prefix) {
        try {
            rows.clear();
            for (String version : versions) {
                rows.add(new VersionRow(
                        version,
                        MindustryGameDownloader.buildDownloadUrl(kind.name(), version, mirrorUrl, prefix),
                        isDownloaded(kind, version)));
            }
            hasNext = versions.size() >= MindustryVersion.PAGE_SIZE;
            pageLabel.setText(bundle("settings.label.page", page));
            model.fireTableDataChanged();
            statusLabel.setText(rows.isEmpty() ? bundle("settings.text.noVersions") : "");
        } catch (Throwable t) {
            statusLabel.setText(bundle("settings.text.loadFailed", t.getMessage()));
        } finally {
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        kindCombo.setEnabled(!loading);
        refreshButton.setEnabled(!loading);
        updateButtons();
        if (loading) {
            statusLabel.setText(bundle("settings.text.loading"));
        }
    }

    private void updateButtons() {
        boolean loading = !kindCombo.isEnabled();
        prevPageButton.setEnabled(!loading && page > 1);
        nextPageButton.setEnabled(!loading && hasNext);
    }

    private Path gameDir() {
        String field = gameJarPathSupplier != null ? gameJarPathSupplier.get() : "";
        String path = field != null && !field.isBlank() ? field : MindustrySettingsState.defaultGameJarPath();
        Path parent = Path.of(path).toAbsolutePath().getParent();
        return parent != null ? parent : Path.of("").toAbsolutePath();
    }

    private boolean isDownloaded(MindustryVersion.MindustryVersionKind kind, String version) {
        return Files.isRegularFile(gameDir().resolve(MindustryGameDownloader.fileNameFor(kind.name(), version)));
    }

    private void handleTableClick(MouseEvent e) {
        int rowIndex = table.rowAtPoint(e.getPoint());
        int column = table.columnAtPoint(e.getPoint());
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        VersionRow row = rows.get(rowIndex);
        if (column == 1) {
            String url = row.url;
            if (url != null && !url.isBlank()) {
                BrowserUtil.browse(url);
            }
        } else if (column == ACTION_COLUMN) {
            downloadVersion(row, rowIndex);
        }
    }

    private void downloadVersion(VersionRow row, int rowIndex) {
        if (row.downloaded || row.downloading) {
            return;
        }
        MindustryVersion.MindustryVersionKind kind = (MindustryVersion.MindustryVersionKind) kindCombo.getSelectedItem();
        if (kind == null) {
            return;
        }
        String mirrorUrl = currentMirrorUrl();
        boolean prefix = currentMirrorPrefix();
        String url = MindustryGameDownloader.buildDownloadUrl(kind.name(), row.version, mirrorUrl, prefix);
        Path target = gameDir().resolve(MindustryGameDownloader.fileNameFor(kind.name(), row.version));

        row.downloading = true;
        model.fireTableRowsUpdated(rowIndex, rowIndex);
        statusLabel.setText(bundle("settings.text.downloading", row.version));

        Promises.runAsync(() -> {
            try {
                MindustryGameDownloader.download(url, target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }).onSuccess(unused -> {
            row.downloading = false;
            row.downloaded = true;
            model.fireTableRowsUpdated(rowIndex, rowIndex);
            statusLabel.setText(bundle("settings.text.downloadComplete", target));
            if (onDownloaded != null) {
                onDownloaded.accept(row.version, target);
            }
        }).onError(error -> {
            row.downloading = false;
            model.fireTableRowsUpdated(rowIndex, rowIndex);
            statusLabel.setText(bundle("settings.text.downloadFailed", error.getMessage()));
        });
    }

    private String actionText(VersionRow row) {
        if (row.downloaded) {
            return bundle("table.action.downloaded");
        }
        if (row.downloading) {
            return bundle("table.action.downloading");
        }
        return bundle("table.action.download");
    }

    private static class VersionRow {
        final String version;
        final String url;
        boolean downloaded;
        boolean downloading;

        VersionRow(String version, String url, boolean downloaded) {
            this.version = version;
            this.url = url;
            this.downloaded = downloaded;
        }
    }

    private class VersionTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return bundle("table.column.version");
            }
            if (column == 1) {
                return bundle("table.column.downloadUrl");
            }
            return bundle("table.column.action");
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            VersionRow row = rows.get(rowIndex);
            if (columnIndex == 0) {
                return row.version;
            }
            if (columnIndex == 1) {
                return row.url != null ? row.url : "";
            }
            return actionText(row);
        }
    }

    private class UrlCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String url = value == null ? "" : value.toString();
            setText(url.isEmpty() ? "" : "<html><a href=''>" + StringUtil.escapeXmlEntities(url) + "</a></html>");
            setToolTipText(url);
            return this;
        }
    }

    private class ActionCellRenderer extends JButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            VersionRow data = rows.get(row);
            setText(actionText(data));
            setEnabled(!data.downloaded && !data.downloading);
            return this;
        }
    }
}

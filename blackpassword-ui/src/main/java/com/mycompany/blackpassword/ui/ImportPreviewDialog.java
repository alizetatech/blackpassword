package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Diálogo de Vista Previa de Importación v2.0 - ANTI-DUPLICADOS.
 * Detecta y alerta sobre pares (Sitio, Usuario) duplicados que ya existan en la base de datos.
 */
public class ImportPreviewDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private boolean confirmed = false;
    private List<Map<String, String>> selectedData = new ArrayList<>();

    public ImportPreviewDialog(Window owner, List<Map<String, String>> data) {
        this(owner, data, Collections.emptySet());
    }

    public ImportPreviewDialog(Window owner, List<Map<String, String>> data, Set<String> existingKeys) {
        super(owner, "Vista Previa de Importación - Detección de Duplicados", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(780, 540));

        int dupCount = 0;
        if (existingKeys != null) {
            for (Map<String, String> row : data) {
                String s = row.getOrDefault("s", "").trim().toUpperCase();
                String u = row.getOrDefault("u", "").trim().toLowerCase();
                if (existingKeys.contains(s + "|||" + u)) {
                    dupCount++;
                }
            }
        }

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setBackground(new Color(248, 249, 250));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblMsg = new JLabel("Revise y seleccione los registros que desea importar a su bóveda:");
        lblMsg.putClientProperty(FlatClientProperties.STYLE, "font: bold 13");
        headerPanel.add(lblMsg, BorderLayout.NORTH);

        if (dupCount > 0) {
            JPanel bannerWarn = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            bannerWarn.putClientProperty(FlatClientProperties.STYLE, "background: #FFF4CE; border: 4,10,4,10, #E2B714, 1, 10");
            JLabel lblWarn = new JLabel("⚠️ ATENCIÓN: Se detectaron " + dupCount + " registros que YA EXISTEN en la base de datos (desmarcados por defecto).");
            lblWarn.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #8A6D3B");
            bannerWarn.add(lblWarn);
            headerPanel.add(bannerWarn, BorderLayout.CENTER);
        }

        // Barra de selección rápida
        JPanel quickSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        quickSelectPanel.setOpaque(false);

        JButton btnSoloNuevos = new JButton("SOLO IMPORTAR NUEVOS");
        btnSoloNuevos.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #E1F0FE; foreground: #0078D4");
        btnSoloNuevos.addActionListener(e -> seleccionarSoloNuevos());
        quickSelectPanel.add(btnSoloNuevos);

        JButton btnMarcarTodos = new JButton("MARCAR TODOS");
        btnMarcarTodos.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: 11");
        btnMarcarTodos.addActionListener(e -> marcarTodos(true));
        quickSelectPanel.add(btnMarcarTodos);

        JButton btnDesmarcarTodos = new JButton("DESMARCAR TODOS");
        btnDesmarcarTodos.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: 11");
        btnDesmarcarTodos.addActionListener(e -> marcarTodos(false));
        quickSelectPanel.add(btnDesmarcarTodos);

        headerPanel.add(quickSelectPanel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE ---
        model = new DefaultTableModel(new Object[]{"IMPORTAR", "SITIO / SERVICIO", "USUARIO / IDENTIDAD", "ESTADO EN BÓVEDA"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Solo el checkbox es editable
            }
        };

        for (Map<String, String> row : data) {
            String s = row.getOrDefault("s", "").trim();
            String u = row.getOrDefault("u", "").trim();
            String key = s.toUpperCase() + "|||" + u.toLowerCase();
            boolean isDuplicate = existingKeys != null && existingKeys.contains(key);

            if (isDuplicate) {
                // Duplicado: desmarcado por defecto para proteger al usuario
                model.addRow(new Object[]{false, s, u, "⚠️ YA EXISTE EN BÓVEDA"});
            } else {
                model.addRow(new Object[]{true, s, u, "✅ NUEVO"});
            }
        }

        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight: 36; showHorizontalLines: true");

        // Renderizador de color para la columna de Estado
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String valStr = value != null ? value.toString() : "";
                if (valStr.startsWith("⚠️")) {
                    c.setForeground(new Color(216, 59, 1));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (valStr.startsWith("✅")) {
                    c.setForeground(new Color(16, 124, 16));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(scroll, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));

        JButton btnDelete = new JButton("ELIMINAR SELECCIONADOS");
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "background: #D83B01; foreground: #FFFFFF; arc: 12; font: bold 12");
        btnDelete.addActionListener(e -> removeSelectedRows());

        JButton btnCancel = new JButton("CANCELAR");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        btnCancel.addActionListener(e -> dispose());

        JButton btnConfirm = new JButton("GUARDAR EN BÓVEDA");
        btnConfirm.putClientProperty(FlatClientProperties.STYLE, "background: #0078D4; foreground: #FFFFFF; arc: 12; font: bold 13");
        btnConfirm.addActionListener(e -> {
            prepareResult(data);
            confirmed = true;
            dispose();
        });

        footer.add(btnDelete);
        footer.add(btnCancel);
        footer.add(btnConfirm);
        add(footer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void marcarTodos(boolean estado) {
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(estado, i, 0);
        }
    }

    private void seleccionarSoloNuevos() {
        for (int i = 0; i < model.getRowCount(); i++) {
            String estado = String.valueOf(model.getValueAt(i, 3));
            model.setValueAt(estado.startsWith("✅"), i, 0);
        }
    }

    private void removeSelectedRows() {
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if ((Boolean) model.getValueAt(i, 0)) {
                model.removeRow(i);
            }
        }
    }

    private void prepareResult(List<Map<String, String>> originalData) {
        selectedData.clear();
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 0)) {
                Map<String, String> row = new HashMap<>();
                String s = (String) model.getValueAt(i, 1);
                String u = (String) model.getValueAt(i, 2);
                row.put("s", s);
                row.put("u", u);

                for (Map<String, String> orig : originalData) {
                    if (orig.getOrDefault("s", "").trim().equalsIgnoreCase(s) &&
                        orig.getOrDefault("u", "").trim().equalsIgnoreCase(u)) {
                        row.put("p", orig.get("p"));
                        break;
                    }
                }
                selectedData.add(row);
            }
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public List<Map<String, String>> getSelectedData() { return selectedData; }
}

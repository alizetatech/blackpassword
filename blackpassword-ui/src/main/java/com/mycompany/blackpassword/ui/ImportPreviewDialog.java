package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.BorderFactory;
import java.awt.Window;

/**
 * Diálogo de Vista Previa de Importación v1.0.
 * Permite filtrar y seleccionar registros antes de guardarlos en la bóveda.
 */
public class ImportPreviewDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private boolean confirmed = false;
    private List<Map<String, String>> selectedData = new ArrayList<>();

    public ImportPreviewDialog(Window owner, List<Map<String, String>> data) {
        super(owner, "Vista Previa de Importación", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(700, 500));

        // --- HEADER ---
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.setBackground(new Color(245, 245, 245));
        JLabel lblMsg = new JLabel("Revise y seleccione los registros que desea importar:");
        lblMsg.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        header.add(lblMsg);
        add(header, BorderLayout.NORTH);

        // --- TABLE ---
        model = new DefaultTableModel(new Object[]{"IMPORTAR", "SERVICIO", "USUARIO", "CONTRASEÑA"}, 0) {
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
            model.addRow(new Object[]{true, row.get("s"), row.get("u"), "••••••••"});
        }

        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.putClientProperty(FlatClientProperties.STYLE, "rowHeight: 35; showHorizontalLines: true");
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scroll, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        
        JButton btnDelete = new JButton("ELIMINAR SELECCIONADOS");
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "background: #D83B01; foreground: #FFFFFF; arc: 12");
        btnDelete.addActionListener(e -> removeSelectedRows());
        
        JButton btnCancel = new JButton("CANCELAR");
        btnCancel.addActionListener(e -> dispose());

        JButton btnConfirm = new JButton("GUARDAR EN BÓVEDA");
        btnConfirm.putClientProperty(FlatClientProperties.STYLE, "background: #0078D4; foreground: #FFFFFF; arc: 12; font: bold");
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

    private void removeSelectedRows() {
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if ((Boolean) model.getValueAt(i, 0)) {
                model.removeRow(i);
            }
        }
    }

    private void prepareResult(List<Map<String, String>> originalData) {
        // Mapeamos los datos filtrados basándonos en el índice de la tabla original
        // NOTA: Para simplicidad en este MVP, asumimos que el orden no cambia drasticamente 
        // o reconstruimos la lista basándonos en los datos visibles si fueran editables.
        // Aquí reconstruiremos la lista de los que quedan marcados:
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 0)) {
                Map<String, String> row = new java.util.HashMap<>();
                row.put("s", (String) model.getValueAt(i, 1));
                row.put("u", (String) model.getValueAt(i, 2));
                
                // Buscamos la contraseña original para este par S/U
                for(Map<String, String> orig : originalData) {
                    if(orig.get("s").equals(model.getValueAt(i, 1)) && orig.get("u").equals(model.getValueAt(i, 2))) {
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

package com.mycompany.blackpassword.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mycompany.blackpassword.services.ServerService;
import com.mycompany.blackpassword.core.ToastNotification;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

/**
 * Visor de la Bóveda Remota (Fortaleza Digital).
 */
public class BovedaRemota extends JInternalFrame {

    private JTable tablaSecretos;
    private DefaultTableModel model;
    private JButton btnRefrescar;

    public BovedaRemota() {
        super("Bóveda Remota - Fortaleza Digital", true, true, true, true);
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        model = new DefaultTableModel(new Object[]{"ID", "Identificador", "Descripción"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        tablaSecretos = new JTable(model);
        btnRefrescar = new JButton("Sincronizar con Fortaleza");
        btnRefrescar.addActionListener(e -> refreshData());

        add(new JScrollPane(tablaSecretos), BorderLayout.CENTER);
        add(btnRefrescar, BorderLayout.SOUTH);
        
        setSize(600, 400);
    }

    private void refreshData() {
        if (!ServerService.getInstance().isConnected()) {
            ToastNotification.show(this, "Debe conectar primero a la Fortaleza", ToastNotification.Type.WARNING);
            return;
        }

        btnRefrescar.setEnabled(false);
        new SwingWorker<JsonArray, Void>() {
            @Override protected JsonArray doInBackground() throws Exception {
                return ServerService.getInstance().getAllSecrets();
            }

            @Override protected void done() {
                try {
                    JsonArray secrets = get();
                    model.setRowCount(0);
                    for (JsonElement el : secrets) {
                        JsonObject obj = el.getAsJsonObject();
                        model.addRow(new Object[]{
                            obj.get("id").getAsString(),
                            obj.get("identifier").getAsString(),
                            obj.get("description").isJsonNull() ? "" : obj.get("description").getAsString()
                        });
                    }
                    ToastNotification.show(BovedaRemota.this, "Sincronización completada", ToastNotification.Type.SUCCESS);
                } catch (Exception e) {
                    ToastNotification.show(BovedaRemota.this, "Error al sincronizar", ToastNotification.Type.ERROR);
                } finally {
                    btnRefrescar.setEnabled(true);
                }
            }
        }.execute();
    }
}

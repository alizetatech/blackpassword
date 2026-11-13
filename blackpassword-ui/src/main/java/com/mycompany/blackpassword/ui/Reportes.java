package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.CloudFunctionsClient;
import com.mycompany.blackpassword.services.IntrusionService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.ReportService;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.table.DefaultTableModel;

/**
 * Centro de Reportes Premium v4.0.
 * Incluye gestión de Alertas de Intrusión Críticas.
 */
public class Reportes extends javax.swing.JInternalFrame {

    private final String userEmail;

    public Reportes(String email) {
        this.userEmail = email;
        initComponents();
        applyPremiumStyle();
        cargarLogsDesdeFirebase();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        jTable1.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; rowHeight: 40; selectionBackground: #E1F0FE");
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0; background: #FFFFFF");
        
        btnExportar.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #107C10; foreground: #FFFFFF; hoverBackground: #006400; borderWidth: 0; font: bold");
        
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4");
        btnAlertas.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FDE7E9; foreground: #D83B01; font: bold");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    private void cargarLogsDesdeFirebase() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (!SecuritySession.isSessionActive()) return;

        if (FirebaseService.isCloudFunctionsActive()) {
            new Thread(() -> {
                try {
                    var items = CloudFunctionsClient.getInstance().listAuditLogs(userEmail);
                    for (var el : items) {
                        if (!el.isJsonObject()) continue;
                        var doc = el.getAsJsonObject();
                        String fecha = "N/A";
                        if (doc.has("timestamp") && doc.get("timestamp").isJsonObject()) {
                            var tsObj = doc.getAsJsonObject("timestamp");
                            if (tsObj.has("_seconds")) {
                                long sec = tsObj.get("_seconds").getAsLong();
                                fecha = new java.util.Date(sec * 1000).toString();
                            }
                        }
                        String accion = doc.has("accion") && !doc.get("accion").isJsonNull() ? doc.get("accion").getAsString() : "N/A";
                        String detalles = "N/A";
                        if (doc.has("detalles_cifrados") && !doc.get("detalles_cifrados").isJsonNull() && !doc.get("detalles_cifrados").getAsString().isEmpty()) {
                            try {
                                byte[] enc = Base64.getDecoder().decode(doc.get("detalles_cifrados").getAsString());
                                byte[] dec = RSALogica.descifrarBytesHibrido(enc, SecuritySession.getPrivateKey());
                                detalles = new String(dec, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                detalles = "[Error de Descifrado]";
                            }
                        } else if (doc.has("detalles_ofuscados") && !doc.get("detalles_ofuscados").isJsonNull()) {
                            detalles = "[Dato Ofuscado]";
                        }
                        String finalF = fecha, finalA = accion, finalD = detalles;
                        javax.swing.SwingUtilities.invokeLater(() -> model.addRow(new Object[]{finalF, finalA, finalD}));
                    }
                } catch (Exception e) {
                    System.err.println("Error cargando logs vía Cloud Functions: " + e.getMessage());
                }
            }).start();
            return;
        }

        try {
            Firestore db = FirebaseService.getFirestore();
            if (db == null) return;
            String emailHash = RSALogica.hashConstantToHex(userEmail);
            
            db.collection("audit_logs")
                .whereEqualTo("email_hash", emailHash)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().get().getDocuments().forEach(doc -> {
                    com.google.cloud.Timestamp ts = doc.getTimestamp("timestamp");
                    String fecha = (ts != null) ? ts.toDate().toString() : "N/A";
                    String accion = doc.getString("accion");
                    String detalles = "N/A";

                    if (doc.contains("detalles_cifrados")) {
                        try {
                            byte[] enc = Base64.getDecoder().decode(doc.getString("detalles_cifrados"));
                            byte[] dec = RSALogica.descifrarBytesHibrido(enc, SecuritySession.getPrivateKey());
                            detalles = new String(dec, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            detalles = "[Error de Descifrado]";
                        }
                    } else if (doc.contains("detalles_ofuscados")) {
                        detalles = "[Dato Ofuscado]";
                    }

                    model.addRow(new Object[]{fecha, accion, detalles});
                });
        } catch (Exception e) {
            System.err.println("Error cargando logs: " + e.getMessage());
        }
    }

    private void verAlertasIntrusion() {
        List<Map<String, Object>> alertas = IntrusionService.getPendingAlerts(userEmail);
        if (alertas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "✅ No hay alertas de intrusión pendientes.", "Seguridad Limpia", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder("🚨 ALERTAS DE SEGURIDAD CRÍTICAS:\n\n");
        for (Map<String, Object> a : alertas) {
            sb.append("📅 ").append(a.get("timestamp")).append("\n");
            sb.append("⚠️ TIPO: ").append(a.get("origen")).append("\n");
            sb.append("📝 ").append(a.get("detalles")).append("\n");
            sb.append("--------------------------------------\n");
        }
        sb.append("\n¿Desea marcar todas las alertas como leídas?");

        int opt = JOptionPane.showConfirmDialog(this, sb.toString(), "CENTRO DE RESPUESTA A INCIDENTES", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            for (Map<String, Object> a : alertas) {
                IntrusionService.markAsRead(a.get("id").toString());
            }
            ToastNotification.show(this, "Alertas despejadas.", ToastNotification.Type.SUCCESS);
        }
    }

    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnExportar = new javax.swing.JButton();
        btnRefrescar = new javax.swing.JButton();
        btnAlertas = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Auditoría de Seguridad");

        lblTitulo.setText("Historial de Actividad Cifrado");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] { "Fecha y Hora", "Acción", "Detalles del Evento" }
        ) {
            boolean[] canEdit = new boolean [] { false, false, false };
            public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit [columnIndex]; }
        });
        jScrollPane1.setViewportView(jTable1);

        btnExportar.setText("EXPORTAR PDF SEGURO");
        btnExportar.addActionListener(evt -> btnExportarActionPerformed(evt));

        btnRefrescar.setText("🔄 Refrescar");
        btnRefrescar.addActionListener(evt -> cargarLogsDesdeFirebase());

        btnAlertas.setText("🚨 ALERTAS DE INTRUSIÓN");
        btnAlertas.addActionListener(evt -> verAlertasIntrusion());

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(lblTitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAlertas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnRefrescar))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCardLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnExportar, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(45, 45, 45))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitulo)
                    .addComponent(btnRefrescar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAlertas, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                .addGap(20, 20, 20)
                .addComponent(btnExportar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(this, pf, "Confirme su PIN para exportar", JOptionPane.OK_CANCEL_OPTION);
        
        if (okCxl == JOptionPane.OK_OPTION && SecuritySession.verifyUserPin(pf.getPassword())) {
            IntrusionService.reportSuccess(userEmail);
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar Reporte de Seguridad");
            fc.setSelectedFile(new File("Reporte_Seguridad_" + System.currentTimeMillis() + ".pdf"));
            
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    List<Map<String, String>> data = new ArrayList<>();
                    for (int i = 0; i < jTable1.getRowCount(); i++) {
                        Map<String, String> row = new HashMap<>();
                        Object vFecha = jTable1.getValueAt(i, 0);
                        Object vAccion = jTable1.getValueAt(i, 1);
                        Object vDetalles = jTable1.getValueAt(i, 2);
                        row.put("fecha", vFecha != null ? vFecha.toString() : "");
                        row.put("accion", vAccion != null ? vAccion.toString() : "");
                        row.put("detalles", vDetalles != null ? vDetalles.toString() : "");
                        data.add(row);
                    }
                    ReportService.generarReporteAuditoria(userEmail, data, fc.getSelectedFile());
                    AuditService.log(userEmail, "REPORT_EXPORT_SUCCESS", "Reporte de auditoría exportado correctamente.");
                    ToastNotification.show(this, "Reporte generado con éxito.", ToastNotification.Type.SUCCESS);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error al exportar: " + e.getMessage());
                }
            }
        } else if (okCxl == JOptionPane.OK_OPTION) {
            IntrusionService.reportFailure(userEmail, "REPORT_EXPORT_PIN");
            ToastNotification.show(this, "PIN incorrecto.", ToastNotification.Type.ERROR);
        }
    }

    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnRefrescar;
    private javax.swing.JButton btnAlertas;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblTitulo;
}

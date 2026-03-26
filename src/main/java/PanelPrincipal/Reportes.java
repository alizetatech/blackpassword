package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.ReportService;
import com.mycompany.blackpassword.SecuritySession;
import com.mycompany.blackpassword.ToastNotification;
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
 * Centro de Reportes Premium con Descifrado RSA en Tiempo Real.
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
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0; background: #FFFFFF");
        
        btnExportar.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #107C10; foreground: #FFFFFF; hoverBackground: #006400; borderWidth: 0; font: bold");
        
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    private void cargarLogsDesdeFirebase() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (!SecuritySession.isSessionActive()) return;

        try {
            Firestore db = FirebaseService.getFirestore();
            String emailHash = RSALogica.hashToHex(userEmail);
            
            db.collection("audit_logs")
                .whereEqualTo("email_hash", emailHash)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().get().getDocuments().forEach(doc -> {
                    String fecha = doc.getTimestamp("timestamp").toDate().toString();
                    String accion = doc.getString("accion");
                    String detalles = "N/A";

                    // Intentar descifrar detalles si existen (Zero-Knowledge)
                    if (doc.contains("detalles_cifrados")) {
                        try {
                            byte[] enc = Base64.getDecoder().decode(doc.getString("detalles_cifrados"));
                            byte[] dec = RSALogica.descifrarBytesHibrido(enc, SecuritySession.getPrivateKey());
                            detalles = new String(dec, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            detalles = "[Error de Descifrado]";
                        }
                    } else if (doc.contains("detalles_publicos")) {
                        detalles = doc.getString("detalles_publicos");
                    }

                    model.addRow(new Object[]{fecha, accion, detalles});
                });
        } catch (Exception e) {
            System.err.println("Error cargando logs: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnExportar = new javax.swing.JButton();
        btnRefrescar = new javax.swing.JButton();

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

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(lblTitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRefrescar))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCardLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnExportar, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(30, 30, 30))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitulo)
                    .addComponent(btnRefrescar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar Reporte de Seguridad");
            fc.setSelectedFile(new File("Reporte_Seguridad_" + System.currentTimeMillis() + ".pdf"));
            
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    List<Map<String, String>> data = new ArrayList<>();
                    for (int i = 0; i < jTable1.getRowCount(); i++) {
                        Map<String, String> row = new HashMap<>();
                        row.put("fecha", jTable1.getValueAt(i, 0).toString());
                        row.put("accion", jTable1.getValueAt(i, 1).toString());
                        row.put("detalles", jTable1.getValueAt(i, 2).toString());
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
            ToastNotification.show(this, "PIN incorrecto.", ToastNotification.Type.ERROR);
        }
    }

    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnRefrescar;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblTitulo;
}

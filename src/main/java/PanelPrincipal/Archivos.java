package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.IntrusionService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Módulo de Gestión de Archivos Cifrados RSA - Edición Zero-Knowledge Metadata.
 */
public class Archivos extends javax.swing.JInternalFrame {

    private final String userEmail;
    private File archivoSeleccionado = null;
    private String idDocumentoEdicion = null;
    private TableRowSorter<DefaultTableModel> sorter;

    public Archivos(String email) {
        this.userEmail = email;
        initComponents();
        applyPremiumStyle();
        setupSearch();
        cargarArchivosDesdeFirebase();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar por nombre o carpeta...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 5,10,5,10");
        
        jTable1.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; rowHeight: 40; selectionBackground: #E1F0FE");
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0; background: #FFFFFF");
        
        txtNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre descriptivo");
        txtCarpeta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Carpeta / Categoría");
        txtRecordatorio.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nota o recordatorio");
        txtRuta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione un archivo...");
        txtRuta.setEditable(false);
        
        btnSubir.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 12; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
        
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #F3F3F3; foreground: #333333");
        btnDescargar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FDE7E9; foreground: #D83B01; font: bold");
        btnEditar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #F3F3F3; foreground: #333333; borderWidth: 0");
        
        jPanelForm.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 20,20,20,20, #FFFFFF, 0, 25");
        btnCancelar.setVisible(false);
    }

    private void setupSearch() {
        sorter = new TableRowSorter<>((DefaultTableModel) jTable1.getModel());
        jTable1.setRowSorter(sorter);
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txtBuscar.getText();
                if (text.trim().length() == 0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
            }
        });
    }

    private void cargarArchivosDesdeFirebase() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (!SecuritySession.isSessionActive()) return;

        try {
            Firestore db = FirebaseService.getFirestore();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
            
            db.collection(path).get().get().getDocuments().forEach(doc -> {
                String nombre = "N/A", carpeta = "N/A";
                
                try {
                    if (doc.contains("metadata_cifrada")) {
                        byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                        String meta = new String(dec, StandardCharsets.UTF_8);
                        for (String p : meta.split("\\|")) {
                            if (p.startsWith("n:")) nombre = p.substring(2);
                            if (p.startsWith("c:")) carpeta = p.substring(2);
                        }
                    } else {
                        // Fallback para datos antiguos en texto plano
                        nombre = doc.getString("nombre");
                        carpeta = doc.getString("carpeta");
                    }
                } catch (Exception e) {
                    nombre = "[Error de Descifrado]";
                }

                model.addRow(new Object[]{
                    nombre,
                    carpeta,
                    doc.getCreateTime() != null ? doc.getCreateTime().toDate().toString() : "N/A",
                    doc.getId()
                });
            });
            jTable1.getColumnModel().getColumn(3).setMinWidth(0);
            jTable1.getColumnModel().getColumn(3).setMaxWidth(0);
        } catch (Exception e) {}
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblTitulo = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanelForm = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtCarpeta = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtRecordatorio = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtRuta = new javax.swing.JTextField();
        btnSeleccionar = new javax.swing.JButton();
        btnSubir = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        btnDescargar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Mis Archivos Blindados");

        lblTitulo.setText("Gestor de Archivos RSA");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] { "Nombre", "Carpeta", "Fecha", "ID" }
        ) {
            boolean[] canEdit = new boolean [] { false, false, false, false };
            public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit [columnIndex]; }
        });
        jScrollPane1.setViewportView(jTable1);

        jLabel1.setText("Nombre del Archivo:");
        jLabel2.setText("Carpeta:");
        jLabel3.setText("Recordatorio:");
        jLabel4.setText("Archivo Local:");

        btnSeleccionar.setText("📂 Buscar");
        btnSeleccionar.addActionListener(evt -> btnSeleccionarActionPerformed(evt));

        btnSubir.setText("SUBIR Y CIFRAR");
        btnSubir.addActionListener(evt -> btnSubirActionPerformed(evt));

        btnCancelar.setText("CANCELAR");
        btnCancelar.addActionListener(evt -> resetForm());

        btnDescargar.setText("📥 Descargar");
        btnDescargar.addActionListener(evt -> btnDescargarActionPerformed(evt));

        btnEliminar.setText("🗑️ Eliminar");
        btnEliminar.addActionListener(evt -> btnEliminarActionPerformed(evt));

        btnEditar.setText("✏️ Editar");
        btnEditar.addActionListener(evt -> btnEditarActionPerformed(evt));

        javax.swing.GroupLayout jPanelFormLayout = new javax.swing.GroupLayout(jPanelForm);
        jPanelForm.setLayout(jPanelFormLayout);
        jPanelFormLayout.setHorizontalGroup(
            jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFormLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtNombre)
                    .addComponent(txtCarpeta)
                    .addComponent(txtRecordatorio)
                    .addGroup(jPanelFormLayout.createSequentialGroup()
                        .addComponent(txtRuta, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSeleccionar))
                    .addComponent(btnSubir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCancelar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelFormLayout.createSequentialGroup()
                        .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelFormLayout.setVerticalGroup(
            jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFormLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCarpeta, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtRecordatorio, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtRuta, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSeleccionar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addComponent(btnSubir, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDescargar, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(30, 30, 30)
                        .addComponent(jPanelForm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(40, 40, 40))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitulo)
                    .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelForm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDescargar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEditar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        pack();
    }

    private void btnSeleccionarActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fc.getSelectedFile();
            txtRuta.setText(archivoSeleccionado.getAbsolutePath());
            if (txtNombre.getText().isEmpty()) txtNombre.setText(archivoSeleccionado.getName());
        }
    }

    private void resetForm() {
        idDocumentoEdicion = null;
        txtNombre.setText(""); txtCarpeta.setText(""); txtRecordatorio.setText(""); txtRuta.setText("");
        archivoSeleccionado = null;
        btnSubir.setText("SUBIR Y CIFRAR");
        btnSubir.putClientProperty(FlatClientProperties.STYLE, "background: #0078D4");
        btnCancelar.setVisible(false);
        btnSeleccionar.setEnabled(true);
    }

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jTable1.getSelectedRow();
        if (fila == -1) return;
        try {
            int modelRow = jTable1.convertRowIndexToModel(fila);
            String docId = jTable1.getModel().getValueAt(modelRow, 3).toString();
            Firestore db = FirebaseService.getFirestore();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
            var doc = db.collection(path).document(docId).get().get();
            
            if (doc.contains("metadata_cifrada")) {
                byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                String meta = new String(dec, StandardCharsets.UTF_8);
                for (String p : meta.split("\\|")) {
                    if (p.startsWith("n:")) txtNombre.setText(p.substring(2));
                    if (p.startsWith("c:")) txtCarpeta.setText(p.substring(2));
                    if (p.startsWith("r:")) txtRecordatorio.setText(p.substring(2));
                }
            } else {
                txtNombre.setText(doc.getString("nombre"));
                txtCarpeta.setText(doc.getString("carpeta"));
                txtRecordatorio.setText(doc.getString("recordatorio"));
            }
            
            txtRuta.setText("[Archivo en la Nube]");
            idDocumentoEdicion = docId;
            btnSubir.setText("ACTUALIZAR DATOS");
            btnSubir.putClientProperty(FlatClientProperties.STYLE, "background: #107C10");
            btnCancelar.setVisible(true);
            btnSeleccionar.setEnabled(false);
            AuditService.log(userEmail, "FILE_EDIT_START", "Iniciada edición de metadatos para un archivo.");
        } catch (Exception e) {}
    }

    private void btnSubirActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText().trim();
        String carpeta = txtCarpeta.getText().trim();
        String recordatorio = txtRecordatorio.getText().trim();
        
        if (nombre.isEmpty()) return;

        try {
            Firestore db = FirebaseService.getFirestore();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
            
            // Cifrar Metadatos (Zero-Knowledge)
            String metadata = "n:" + nombre + "|c:" + carpeta + "|r:" + recordatorio;
            byte[] encMeta = RSALogica.cifrarBytesHibrido(metadata.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
            
            Map<String, Object> data = new HashMap<>();
            data.put("metadata_cifrada", Base64.getEncoder().encodeToString(encMeta));

            if (idDocumentoEdicion == null) {
                if (archivoSeleccionado == null) return;
                byte[] encryptedBytes = RSALogica.cifrarBytesHibrido(Files.readAllBytes(archivoSeleccionado.toPath()), SecuritySession.getPublicKey());
                data.put("contenido_cifrado", Base64.getEncoder().encodeToString(encryptedBytes));
                data.put("extension_original", archivoSeleccionado.getName().substring(archivoSeleccionado.getName().lastIndexOf('.')));
                db.collection(path).add(data).get();
                AuditService.log(userEmail, "FILE_UPLOAD", "Archivo subido con metadatos anónimos.");
            } else {
                db.collection(path).document(idDocumentoEdicion).update(data).get();
                AuditService.log(userEmail, "FILE_METADATA_UPDATE", "Metadatos anónimos actualizados.");
            }
            
            resetForm(); cargarArchivosDesdeFirebase();
            JOptionPane.showMessageDialog(this, "Operación exitosa.");
        } catch (Exception e) { 
            AuditService.log(userEmail, "FILE_OPERATION_ERROR", "Error en operación de archivo: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); 
        }
    }

    private void btnDescargarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jTable1.getSelectedRow();
        if (fila == -1) return;
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, pf, "PIN para descargar", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            char[] pin = pf.getPassword();
            if (SecuritySession.verifyUserPin(pin)) {
                IntrusionService.reportSuccess(userEmail);
                try {
                    int modelRow = jTable1.convertRowIndexToModel(fila);
                    String docId = jTable1.getModel().getValueAt(modelRow, 3).toString();
                    String nombreSugerido = jTable1.getModel().getValueAt(modelRow, 0).toString();
                    
                    String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
                    var doc = FirebaseService.getFirestore().collection(path).document(docId).get().get();
                    byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("contenido_cifrado")), SecuritySession.getPrivateKey());
                    
                    JFileChooser fc = new JFileChooser();
                    fc.setSelectedFile(new File(nombreSugerido));
                    if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        Files.write(fc.getSelectedFile().toPath(), dec);
                        AuditService.log(userEmail, "FILE_DOWNLOAD", "Archivo descargado y descifrado.");
                        JOptionPane.showMessageDialog(this, "Archivo recuperado.");
                    }
                } catch (Exception e) {
                    AuditService.log(userEmail, "FILE_DOWNLOAD_ERROR", "Error al descargar archivo: " + e.getMessage());
                }
            } else {
                IntrusionService.reportFailure(userEmail, "FILE_PIN_DOWNLOAD");
                JOptionPane.showMessageDialog(this, "PIN incorrecto. Acción cancelada.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jTable1.getSelectedRow();
        if (fila == -1 || JOptionPane.showConfirmDialog(this, "¿Eliminar permanentemente?") != JOptionPane.YES_OPTION) return;
        try {
            int modelRow = jTable1.convertRowIndexToModel(fila);
            String docId = jTable1.getModel().getValueAt(modelRow, 3).toString();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
            FirebaseService.getFirestore().collection(path).document(docId).delete().get();
            AuditService.log(userEmail, "FILE_DELETE", "Archivo eliminado de la nube.");
            cargarArchivosDesdeFirebase();
            if (docId.equals(idDocumentoEdicion)) resetForm();
        } catch (Exception e) {}
    }

    private javax.swing.JButton btnDescargar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnSeleccionar;
    private javax.swing.JButton btnSubir;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanelForm;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtCarpeta;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtRecordatorio;
    private javax.swing.JTextField txtRuta;
    private javax.swing.JTextField txtBuscar;
}

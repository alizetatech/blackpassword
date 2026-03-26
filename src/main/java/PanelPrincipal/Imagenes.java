package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.JDesktopPane;

/**
 * Galería de Imágenes v9.9-LUXURY: Carga de imagen en ImageViewer.
 */
public class Imagenes extends javax.swing.JInternalFrame {

    private final String userEmail;
    private File archivoSeleccionado;
    private TableRowSorter<DefaultTableModel> sorter;
    private String idDocumentoEdicion = null;
    private MenuPrincipal parentMenu; // Referencia directa al MenuPrincipal

    public Imagenes(String email, MenuPrincipal parentMenu) {
        this.userEmail = email;
        this.parentMenu = parentMenu; // Asignar la referencia
        initComponents();
        applyPremiumStyle();
        setupSearch();
        cargarImagenesDesdeFirebase();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar por nombre o carpeta...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; margin: 5,10,5,10");
        
        tablaImagenes.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; rowHeight: 40; selectionBackground: #E1F0FE");
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0; background: #FFFFFF");
        
        txtNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre descriptivo");
        txtCarpeta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Carpeta / Categoría");
        txtRecordatorio.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nota o recordatorio");
        
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #F3F3F3; foreground: #333333");
        btnSubir.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
        btnEditar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnVer.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnDescargar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #FDE7E9; foreground: #D83B01; font: bold");
        
        jPanelForm.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 20,20,20,20, #FFFFFF, 0, 25");
    }

    private void setupSearch() {
        sorter = new TableRowSorter<>((DefaultTableModel) tablaImagenes.getModel());
        tablaImagenes.setRowSorter(sorter);
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txtBuscar.getText();
                sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 0, 1));
            }
        });
    }

    private void cargarImagenesDesdeFirebase() {
        DefaultTableModel model = (DefaultTableModel) tablaImagenes.getModel();
        model.setRowCount(0);
        if (!SecuritySession.isSessionActive()) return;

        try {
            Firestore db = FirebaseService.getFirestore();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
            
            ApiFuture<QuerySnapshot> future = db.collection(path).orderBy("timestamp", Query.Direction.DESCENDING).get();
            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                String nombre = "[Error de Descifrado]", carpeta = "N/A";
                try {
                    byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                    String meta = new String(dec, StandardCharsets.UTF_8);
                    for (String p : meta.split("\\|")) {
                        if (p.startsWith("n:")) nombre = p.substring(2);
                        if (p.startsWith("c:")) carpeta = p.substring(2);
                    }
                } catch (Exception e) {}
                model.addRow(new Object[]{nombre, carpeta, doc.getId()});
            }
            tablaImagenes.getColumnModel().getColumn(2).setMinWidth(0);
            tablaImagenes.getColumnModel().getColumn(2).setMaxWidth(0);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar imágenes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanelForm = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtCarpeta = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtRecordatorio = new javax.swing.JTextArea();
        btnSeleccionar = new javax.swing.JButton();
        lblPreview = new javax.swing.JLabel();
        btnSubir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaImagenes = new javax.swing.JTable();
        lblTitulo = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        btnEditar = new javax.swing.JButton();
        btnVer = new javax.swing.JButton();
        btnDescargar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Galería de Imágenes Cifradas");

        jLabel1.setText("Nombre:");
        jLabel2.setText("Carpeta:");
        jLabel3.setText("Recordatorio:");
        txtRecordatorio.setColumns(20);
        txtRecordatorio.setRows(5);
        jScrollPane2.setViewportView(txtRecordatorio);
        btnSeleccionar.setText("Seleccionar Imagen...");
        btnSeleccionar.addActionListener(this::btnSeleccionarActionPerformed);
        lblPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPreview.setText("Vista Previa");
        lblPreview.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnSubir.setText("SUBIR Y CIFRAR");
        btnSubir.addActionListener(this::btnSubirActionPerformed);

        javax.swing.GroupLayout jPanelFormLayout = new javax.swing.GroupLayout(jPanelForm);
        jPanelForm.setLayout(jPanelFormLayout);
        jPanelFormLayout.setHorizontalGroup(
            jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFormLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtNombre)
                    .addComponent(txtCarpeta)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .addComponent(btnSeleccionar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnSubir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelFormLayout.createSequentialGroup()
                        .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelFormLayout.setVerticalGroup(
            jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFormLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCarpeta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSeleccionar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSubir, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tablaImagenes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] { "Nombre", "Carpeta", "ID" }
        ) {
            boolean[] canEdit = new boolean [] { false, false, false };
            public boolean isCellEditable(int rowIndex, int columnIndex) { return canEdit [columnIndex]; }
        });
        jScrollPane1.setViewportView(tablaImagenes);

        lblTitulo.setText("Galería de Imágenes");
        btnEditar.setText("✏️ Editar");
        btnEditar.addActionListener(this::btnEditarActionPerformed);
        btnVer.setText("👁️ Ver");
        btnVer.addActionListener(this::btnVerActionPerformed);
        btnDescargar.setText("📥 Descargar");
        btnDescargar.addActionListener(this::btnDescargarActionPerformed);
        btnEliminar.setText("🗑️ Eliminar");
        btnEliminar.addActionListener(this::btnEliminarActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnEditar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnVer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDescargar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEliminar)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addComponent(jPanelForm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitulo)
                    .addComponent(txtBuscar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnEditar)
                            .addComponent(btnVer)
                            .addComponent(btnDescargar)
                            .addComponent(btnEliminar)))
                    .addComponent(jPanelForm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(20, 20, 20))
        );

        pack();
    }
    
    private void btnSeleccionarActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fc.getSelectedFile();
            txtNombre.setText(archivoSeleccionado.getName());
            try {
                ImageIcon originalIcon = new ImageIcon(archivoSeleccionado.getAbsolutePath());
                Image scaledImage = originalIcon.getImage().getScaledInstance(lblPreview.getWidth(), lblPreview.getHeight(), Image.SCALE_SMOOTH);
                lblPreview.setIcon(new ImageIcon(scaledImage));
            } catch (Exception e) {
                lblPreview.setIcon(null);
                lblPreview.setText("Error al cargar vista previa.");
            }
        }
    }

    private void btnSubirActionPerformed(java.awt.event.ActionEvent evt) {
        if (idDocumentoEdicion == null && archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una imagen para subir.");
            return;
        }
        
        String nombre = txtNombre.getText().trim();
        String carpeta = txtCarpeta.getText().trim();
        String recordatorio = txtRecordatorio.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }

        try {
            String metadata = "n:" + nombre + "|c:" + carpeta + "|r:" + recordatorio;
            byte[] encMeta = RSALogica.cifrarBytesHibrido(metadata.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
            
            Map<String, Object> docData = new HashMap<>();
            docData.put("metadata_cifrada", Base64.getEncoder().encodeToString(encMeta));
            docData.put("timestamp", com.google.cloud.Timestamp.now());

            Firestore db = FirebaseService.getFirestore();
            String collectionPath = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";

            if (idDocumentoEdicion == null) { // Subir nueva imagen
                byte[] fileBytes = Files.readAllBytes(archivoSeleccionado.toPath());
                byte[] encryptedBytes = RSALogica.cifrarBytesHibrido(fileBytes, SecuritySession.getPublicKey());
                
                String storagePath = "imagenes/" + RSALogica.hashToHex(userEmail) + "/" + UUID.randomUUID().toString();
                Bucket bucket = FirebaseService.getStorageBucket();
                bucket.create(storagePath, encryptedBytes, "application/octet-stream");
                
                docData.put("storage_path", storagePath);
                db.collection(collectionPath).add(docData).get();
                AuditService.log(userEmail, "IMAGE_UPLOAD_SUCCESS", "Imagen subida a la bóveda.");
            } else { // Actualizar metadatos
                db.collection(collectionPath).document(idDocumentoEdicion).update(docData).get();
                AuditService.log(userEmail, "IMAGE_METADATA_UPDATE", "Metadatos de imagen actualizados.");
            }
            
            resetForm();
            cargarImagenesDesdeFirebase();
        } catch (Exception e) {
            e.printStackTrace();
            AuditService.log(userEmail, "IMAGE_OPERATION_ERROR", e.getMessage());
            JOptionPane.showMessageDialog(this, "Error en la operación: " + e.getMessage());
        }
    }

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = tablaImagenes.getSelectedRow();
        if (fila == -1) return;
        try {
            int modelRow = tablaImagenes.convertRowIndexToModel(fila);
            String docId = tablaImagenes.getModel().getValueAt(modelRow, 2).toString();
            Firestore db = FirebaseService.getFirestore();
            String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
            var doc = db.collection(path).document(docId).get().get();
            
            if (doc.contains("metadata_cifrada")) {
                byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                String meta = new String(dec, StandardCharsets.UTF_8);
                for (String p : meta.split("\\|")) {
                    if (p.startsWith("n:")) txtNombre.setText(p.substring(2));
                    if (p.startsWith("c:")) txtCarpeta.setText(p.substring(2));
                    if (p.startsWith("r:")) txtRecordatorio.setText(p.substring(2));
                }
            }
            
            lblPreview.setIcon(null);
            lblPreview.setText("[Imagen en la Nube]");
            idDocumentoEdicion = docId;
            btnSubir.setText("ACTUALIZAR DATOS");
            btnSubir.putClientProperty(FlatClientProperties.STYLE, "background: #107C10");
            btnSeleccionar.setEnabled(false);
            AuditService.log(userEmail, "IMAGE_EDIT_START", "Iniciada edición de metadatos para una imagen.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar datos para edición: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnVerActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = tablaImagenes.getSelectedRow();
        if (fila == -1) return;
        try {
            int modelRow = tablaImagenes.convertRowIndexToModel(fila);
            String docId = tablaImagenes.getModel().getValueAt(modelRow, 2).toString();
            String storagePath = getStoragePathForImage(docId);
            
            Bucket bucket = FirebaseService.getStorageBucket();
            Blob blob = bucket.get(storagePath);
            if (blob == null) {
                JOptionPane.showMessageDialog(this, "La imagen no se encontró en la bóveda.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            byte[] encryptedBytes = blob.getContent();
            byte[] decryptedBytes = RSALogica.descifrarBytesHibrido(encryptedBytes, SecuritySession.getPrivateKey());
            
            File tempFile = File.createTempFile("bp_img_", ".tmp");
            Files.write(tempFile.toPath(), decryptedBytes);
            tempFile.deleteOnExit(); // Asegurarse de que se borre al cerrar la JVM
            
            JDesktopPane desktop = getDesktopPane(); 
            if (desktop != null) {
                ImageViewer viewer = new ImageViewer(this.parentMenu, tempFile);
                desktop.add(viewer);
                viewer.setVisible(true);
                viewer.pack(); // Empaquetar para obtener dimensiones válidas
                viewer.loadImage(); // Cargar la imagen después de empaquetar
                viewer.toFront();
                AuditService.log(userEmail, "IMAGE_VIEW", "Imagen visualizada.");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo encontrar el escritorio para abrir el visor.", "Error de UI", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al ver la imagen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnDescargarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = tablaImagenes.getSelectedRow();
        if (fila == -1) return;
        try {
            int modelRow = tablaImagenes.convertRowIndexToModel(fila);
            String docId = tablaImagenes.getModel().getValueAt(modelRow, 2).toString();
            String nombreSugerido = tablaImagenes.getModel().getValueAt(modelRow, 0).toString();
            String storagePath = getStoragePathForImage(docId);
            
            Bucket bucket = FirebaseService.getStorageBucket();
            Blob blob = bucket.get(storagePath);
            if (blob == null) {
                JOptionPane.showMessageDialog(this, "La imagen no se encontró en la bóveda.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            byte[] encryptedBytes = blob.getContent();
            byte[] decryptedBytes = RSALogica.descifrarBytesHibrido(encryptedBytes, SecuritySession.getPrivateKey());
            
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(nombreSugerido + ".png")); // Sugerir extensión
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Files.write(fc.getSelectedFile().toPath(), decryptedBytes);
                AuditService.log(userEmail, "IMAGE_DOWNLOAD", "Imagen descargada y descifrada.");
                JOptionPane.showMessageDialog(this, "Imagen recuperada.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al descargar la imagen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = tablaImagenes.getSelectedRow();
        if (fila == -1 || JOptionPane.showConfirmDialog(this, "¿Eliminar permanentemente esta imagen?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            int modelRow = tablaImagenes.convertRowIndexToModel(fila);
            String docId = tablaImagenes.getModel().getValueAt(modelRow, 2).toString();
            String storagePath = getStoragePathForImage(docId);
            
            // 1. Eliminar de Cloud Storage
            Bucket bucket = FirebaseService.getStorageBucket();
            if (bucket.get(storagePath) != null) {
                bucket.get(storagePath).delete();
                System.out.println("[Imagenes] Eliminado de Storage: " + storagePath);
            }
            
            // 2. Eliminar de Firestore
            Firestore db = FirebaseService.getFirestore();
            String collectionPath = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
            db.collection(collectionPath).document(docId).delete().get();
            
            AuditService.log(userEmail, "IMAGE_DELETE", "Imagen eliminada de la nube.");
            cargarImagenesDesdeFirebase();
            resetForm();
            JOptionPane.showMessageDialog(this, "Imagen eliminada con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al eliminar la imagen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String getStoragePathForImage(String docId) throws Exception {
        Firestore db = FirebaseService.getFirestore();
        String collectionPath = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
        var doc = db.collection(collectionPath).document(docId).get().get();
        return doc.getString("storage_path");
    }

    private void resetForm() {
        txtNombre.setText("");
        txtCarpeta.setText("");
        txtRecordatorio.setText("");
        lblPreview.setIcon(null);
        lblPreview.setText("Vista Previa");
        archivoSeleccionado = null;
        idDocumentoEdicion = null;
        btnSubir.setText("SUBIR Y CIFRAR");
        btnSubir.putClientProperty(FlatClientProperties.STYLE, "background: #0078D4");
        btnSeleccionar.setEnabled(true);
    }
    
    private javax.swing.JButton btnDescargar;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnSeleccionar;
    private javax.swing.JButton btnSubir;
    private javax.swing.JButton btnVer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel jPanelForm;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblPreview;
    private javax.swing.JTable tablaImagenes;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtCarpeta;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextArea txtRecordatorio;
}

package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.storage.Blob;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.ToastNotification;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Galería de Imágenes v10.0-LUXURY: Diseño unificado y responsivo.
 * Integrado con el estándar visual de la Bóveda.
 */
public class Imagenes extends javax.swing.JInternalFrame {

    private final String userEmail;
    private File archivoSeleccionado;
    private String idDocumentoEdicion = null;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private final MenuPrincipal parentMenu;

    private JTextField txtNombre, txtCarpeta, txtBuscar;
    private JTextArea txtRecordatorio;
    private JLabel lblPreview, lblTitulo;
    private JTable table;
    private JButton btnSubir, btnSeleccionar, btnCancelar, btnEditar, btnVer, btnDescargar, btnEliminar, btnVolver, btnRefresh;

    public Imagenes(String email, MenuPrincipal parentMenu) {
        super("Galería de Imágenes Blindadas", false, false, false, false);
        this.userEmail = email;
        this.parentMenu = parentMenu;
        validarIntegridad();
        
        try {
            initComponentsCustom();
            applyPremiumStyle();
            setupSearch();
            cargarImagenesDesdeFirebase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validarIntegridad() {
        if (!SecuritySession.isIdentityValid(userEmail)) {
            AuditService.log(userEmail, "IDENTITY_VIOLATION_IMAGENES", "Intento de acceso con identidad no válida.");
            JOptionPane.showMessageDialog(this, "🚨 ERROR DE SEGURIDAD:\nIdentidad digital no sincronizada.", "Alerta", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
        }
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 22; foreground: #0078D4");
        
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight: 48;"
                + "showHorizontalLines: true;"
                + "intercellSpacing: 0,1;"
                + "selectionBackground: #E5F3FF;"
                + "selectionForeground: #000000;"
                + "showVerticalLines: false;"
                + "arc: 12");
        
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold; background: #F8F9FA; separatorColor: #E0E0E0");

        String fieldStyle = "arc: 12; margin: 6,12,6,12; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4";
        txtNombre.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtCarpeta.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtRecordatorio.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4");
        
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar imágenes por nombre o carpeta...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; margin: 5,15,5,15; background: #FFFFFF");

        btnSubir.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        
        String actionStyle = "arc: 14; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12; textAlign: left; margin: 0,20,0,20";
        btnVer.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnEditar.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnDescargar.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, actionStyle + "; foreground: #D83B01; hoverBackground: #FDE7E9");
        
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnVolver.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; arc: 12; foreground: #0078D4; font: bold 11; background: #F3F9FF");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12; margin: 0,15,0,15");

        try {
            btnSubir.setIcon(new FlatSVGIcon("img/images.svg", 20, 20));
            btnVer.setIcon(new FlatSVGIcon("img/unlock.svg", 18, 18));
            btnEditar.setIcon(new FlatSVGIcon("img/palette.svg", 18, 18));
            btnDescargar.setIcon(new FlatSVGIcon("img/files.svg", 18, 18));
            btnEliminar.setIcon(new FlatSVGIcon("img/panic.svg", 18, 18));
            btnVolver.setIcon(new FlatSVGIcon("img/vault.svg", 18, 18));
        } catch (Exception e) {}
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout(0, 0));

        // --- HEADER ---
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(Color.WHITE);
        panelHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        panelHeader.setPreferredSize(new Dimension(getWidth(), 60));
        
        JPanel panelLeftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        panelLeftHeader.setOpaque(false);
        btnVolver = new JButton("DASHBOARD");
        btnVolver.addActionListener(e -> dispose());
        panelLeftHeader.add(btnVolver);
        lblTitulo = new JLabel("GALERÍA DE IMÁGENES BLINDADAS");
        panelLeftHeader.add(lblTitulo);
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);
        
        JPanel panelRightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        panelRightHeader.setOpaque(false);
        btnRefresh = new JButton("SINCRONIZAR GALERÍA");
        btnRefresh.addActionListener(e -> cargarImagenesDesdeFirebase());
        panelRightHeader.add(btnRefresh);
        panelHeader.add(panelRightHeader, BorderLayout.EAST);
        
        add(panelHeader, BorderLayout.NORTH);

        // --- SIDE PANEL (WEST) ---
        JPanel panelSide = new JPanel(new BorderLayout());
        panelSide.setBackground(new Color(250, 250, 250));
        panelSide.setPreferredSize(new Dimension(280, 0));
        panelSide.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setOpaque(false);
        panelForm.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        JLabel lblSec1 = new JLabel("SUBIR IMAGEN");
        lblSec1.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        panelForm.add(lblSec1, gbc);

        txtNombre = new JTextField(); txtNombre.setPreferredSize(new Dimension(100, 40));
        txtCarpeta = new JTextField(); txtCarpeta.setPreferredSize(new Dimension(100, 40));
        txtRecordatorio = new JTextArea(3, 20);
        txtRecordatorio.setLineWrap(true);
        txtRecordatorio.setWrapStyleWord(true);
        
        addLabeledField(panelForm, "Nombre:", txtNombre, gbc, row++); row++;
        addLabeledField(panelForm, "Carpeta:", txtCarpeta, gbc, row++); row++;
        
        JLabel lr = new JLabel("Nota:"); lr.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.insets = new Insets(5, 0, 2, 0); gbc.gridy = row++;
        panelForm.add(lr, gbc);
        JScrollPane scrollNote = new JScrollPane(txtRecordatorio);
        scrollNote.setPreferredSize(new Dimension(100, 80));
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        panelForm.add(scrollNote, gbc);

        btnSeleccionar = new JButton("SELECCIONAR IMAGEN");
        btnSeleccionar.setPreferredSize(new Dimension(100, 40));
        btnSeleccionar.addActionListener(e -> seleccionarImagen());
        gbc.insets = new Insets(5, 0, 10, 0); gbc.gridy = row++;
        panelForm.add(btnSeleccionar, gbc);

        lblPreview = new JLabel("Vista Previa", SwingConstants.CENTER);
        lblPreview.setPreferredSize(new Dimension(230, 160));
        lblPreview.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        gbc.gridy = row++;
        panelForm.add(lblPreview, gbc);

        btnSubir = new JButton("SUBIR Y CIFRAR");
        btnSubir.setPreferredSize(new Dimension(100, 45));
        btnSubir.addActionListener(e -> subirImagen());
        gbc.insets = new Insets(15, 0, 15, 0); gbc.gridy = row++;
        panelForm.add(btnSubir, gbc);

        btnCancelar = new JButton("CANCELAR EDICIÓN");
        btnCancelar.setVisible(false);
        btnCancelar.addActionListener(e -> resetForm());
        gbc.gridy = row++;
        panelForm.add(btnCancelar, gbc);

        JLabel lblSec2 = new JLabel("ACCIONES");
        lblSec2.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(15, 0, 10, 0); gbc.gridy = row++;
        panelForm.add(lblSec2, gbc);

        btnVer = new JButton("VISUALIZAR IMAGEN");
        btnVer.addActionListener(e -> verImagen());
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        panelForm.add(btnVer, gbc);

        btnEditar = new JButton("CAMBIAR METADATOS");
        btnEditar.addActionListener(e -> editarMetadata());
        gbc.gridy = row++;
        panelForm.add(btnEditar, gbc);

        btnDescargar = new JButton("DECARGAR ORIGINAL");
        btnDescargar.addActionListener(e -> descargarImagen());
        gbc.gridy = row++;
        panelForm.add(btnDescargar, gbc);

        btnEliminar = new JButton("BORRAR DE LA NUBE");
        btnEliminar.addActionListener(e -> eliminarImagen());
        gbc.insets = new Insets(20, 0, 0, 0); gbc.gridy = row++;
        panelForm.add(btnEliminar, gbc);

        gbc.weighty = 1.0; gbc.gridy = row++;
        panelForm.add(new JLabel(""), gbc);

        JScrollPane sideScroll = new JScrollPane(panelForm);
        sideScroll.setBorder(null);
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        panelSide.add(sideScroll, BorderLayout.CENTER);
        add(panelSide, BorderLayout.WEST);

        // --- CENTER PANEL (TABLE) ---
        JPanel panelCenter = new JPanel(new BorderLayout(0, 15));
        panelCenter.setOpaque(false);
        panelCenter.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        txtBuscar = new JTextField();
        txtBuscar.setPreferredSize(new Dimension(100, 42));
        panelCenter.add(txtBuscar, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"NOMBRE", "CARPETA / CATEGORÍA", "ID"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        panelCenter.add(scroll, BorderLayout.CENTER);
        
        add(panelCenter, BorderLayout.CENTER);
    }

    private void addLabeledField(JPanel p, String label, JTextField f, GridBagConstraints gbc, int row) {
        gbc.insets = new Insets(5, 0, 2, 0); gbc.gridy = row;
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l, gbc);
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row + 1;
        p.add(f, gbc);
    }

    private void setupSearch() {
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String text = txtBuscar.getText();
                if (text.trim().isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
    }

    private void cargarImagenesDesdeFirebase() {
        validarIntegridad();
        model.setRowCount(0);
        new Thread(() -> {
            try {
                Firestore db = FirebaseService.getFirestore();
                String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
                ApiFuture<QuerySnapshot> future = db.collection(path).orderBy("timestamp", Query.Direction.DESCENDING).get();
                for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                    String nombre = "[Error]", carpeta = "N/A";
                    try {
                        byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                        String meta = new String(dec, StandardCharsets.UTF_8);
                        for (String p : meta.split("\\|")) {
                            if (p.startsWith("n:")) nombre = p.substring(2);
                            if (p.startsWith("c:")) carpeta = p.substring(2);
                        }
                    } catch (Exception e) {}
                    String finalN = nombre, finalC = carpeta;
                    java.awt.EventQueue.invokeLater(() -> model.addRow(new Object[]{finalN, finalC, doc.getId()}));
                }
                java.awt.EventQueue.invokeLater(() -> {
                    table.getColumnModel().getColumn(2).setMinWidth(0);
                    table.getColumnModel().getColumn(2).setMaxWidth(0);
                });
            } catch (Exception e) {}
        }).start();
    }

    private void seleccionarImagen() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fc.getSelectedFile();
            txtNombre.setText(archivoSeleccionado.getName());
            try {
                ImageIcon originalIcon = new ImageIcon(archivoSeleccionado.getAbsolutePath());
                Image scaledImage = originalIcon.getImage().getScaledInstance(lblPreview.getWidth(), lblPreview.getHeight(), Image.SCALE_SMOOTH);
                lblPreview.setIcon(new ImageIcon(scaledImage));
                lblPreview.setText("");
            } catch (Exception e) {
                lblPreview.setIcon(null); lblPreview.setText("Error Preview");
            }
        }
    }

    private void subirImagen() {
        validarIntegridad();
        if (idDocumentoEdicion == null && archivoSeleccionado == null) return;
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) return;

        new Thread(() -> {
            try {
                String metadata = "n:" + nombre + "|c:" + txtCarpeta.getText() + "|r:" + txtRecordatorio.getText();
                byte[] encMeta = RSALogica.cifrarBytesHibrido(metadata.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                Map<String, Object> docData = new HashMap<>();
                docData.put("metadata_cifrada", Base64.getEncoder().encodeToString(encMeta));
                docData.put("timestamp", com.google.cloud.Timestamp.now());

                Firestore db = FirebaseService.getFirestore();
                String collectionPath = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";

                if (idDocumentoEdicion == null) {
                    byte[] fileBytes = Files.readAllBytes(archivoSeleccionado.toPath());
                    byte[] encryptedBytes = RSALogica.cifrarBytesHibrido(fileBytes, SecuritySession.getPublicKey());
                    String storagePath = "imagenes/" + RSALogica.hashToHex(userEmail) + "/" + UUID.randomUUID().toString();
                    FirebaseService.getStorageBucket().create(storagePath, encryptedBytes, "application/octet-stream");
                    docData.put("storage_path", storagePath);
                    db.collection(collectionPath).add(docData).get();
                } else {
                    db.collection(collectionPath).document(idDocumentoEdicion).update(docData).get();
                }
                
                java.awt.EventQueue.invokeLater(() -> {
                    resetForm(); cargarImagenesDesdeFirebase();
                    ToastNotification.show(this, "Imagen procesada", ToastNotification.Type.SUCCESS);
                });
            } catch (Exception e) {
                java.awt.EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()));
            }
        }).start();
    }

    private void editarMetadata() {
        int fila = table.getSelectedRow();
        if (fila == -1) return;
        try {
            String docId = model.getValueAt(table.convertRowIndexToModel(fila), 2).toString();
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
            idDocumentoEdicion = docId;
            btnSubir.setText("ACTUALIZAR");
            btnCancelar.setVisible(true);
            btnSeleccionar.setEnabled(false);
            lblPreview.setIcon(null); lblPreview.setText("[Nube]");
        } catch (Exception e) {}
    }

    private void verImagen() {
        int fila = table.getSelectedRow();
        if (fila == -1) return;
        new Thread(() -> {
            try {
                String docId = model.getValueAt(table.convertRowIndexToModel(fila), 2).toString();
                String storagePath = getStoragePath(docId);
                Blob blob = FirebaseService.getStorageBucket().get(storagePath);
                byte[] dec = RSALogica.descifrarBytesHibrido(blob.getContent(), SecuritySession.getPrivateKey());
                File temp = File.createTempFile("bp_img_", ".png");
                Files.write(temp.toPath(), dec);
                temp.deleteOnExit();
                
                java.awt.EventQueue.invokeLater(() -> {
                    ImageViewer viewer = new ImageViewer(parentMenu, temp);
                    getDesktopPane().add(viewer);
                    viewer.setVisible(true); viewer.loadImage();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void descargarImagen() {
        int fila = table.getSelectedRow();
        if (fila == -1) return;
        new Thread(() -> {
            try {
                String docId = model.getValueAt(table.convertRowIndexToModel(fila), 2).toString();
                Blob blob = FirebaseService.getStorageBucket().get(getStoragePath(docId));
                byte[] dec = RSALogica.descifrarBytesHibrido(blob.getContent(), SecuritySession.getPrivateKey());
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setSelectedFile(new File(model.getValueAt(fila, 0).toString() + ".png"));
                    if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try { Files.write(fc.getSelectedFile().toPath(), dec); } catch (Exception e) {}
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

    private void eliminarImagen() {
        int fila = table.getSelectedRow();
        if (fila == -1 || JOptionPane.showConfirmDialog(this, "¿Borrar imagen?") != JOptionPane.YES_OPTION) return;
        new Thread(() -> {
            try {
                String docId = model.getValueAt(table.convertRowIndexToModel(fila), 2).toString();
                String sp = getStoragePath(docId);
                FirebaseService.getStorageBucket().get(sp).delete();
                String coll = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
                FirebaseService.getFirestore().collection(coll).document(docId).delete().get();
                java.awt.EventQueue.invokeLater(() -> { cargarImagenesDesdeFirebase(); resetForm(); });
            } catch (Exception e) {}
        }).start();
    }

    private String getStoragePath(String docId) throws Exception {
        String coll = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/imagenes";
        return FirebaseService.getFirestore().collection(coll).document(docId).get().get().getString("storage_path");
    }

    private void resetForm() {
        txtNombre.setText(""); txtCarpeta.setText(""); txtRecordatorio.setText("");
        lblPreview.setIcon(null); lblPreview.setText("Vista Previa");
        archivoSeleccionado = null; idDocumentoEdicion = null;
        btnSubir.setText("SUBIR Y CIFRAR"); btnCancelar.setVisible(false); btnSeleccionar.setEnabled(true);
    }
}

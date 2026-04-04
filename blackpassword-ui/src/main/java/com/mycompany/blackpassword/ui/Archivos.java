package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.cloud.firestore.Firestore;
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
import java.util.*;

/**
 * Módulo de Gestión de Archivos Cifrados RSA - LUXURY EDITION.
 * Unificado con el diseño de la Bóveda.
 */
public class Archivos extends javax.swing.JInternalFrame {

    private final String userEmail;
    private File archivoSeleccionado = null;
    private String idDocumentoEdicion = null;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField txtNombre, txtCarpeta, txtRecordatorio, txtRuta, txtBuscar;
    private JTable table;
    private JButton btnSubir, btnSeleccionar, btnCancelar, btnDescargar, btnEliminar, btnEditar, btnVolver, btnRefresh;
    private JLabel lblTitulo;

    public Archivos(String email) {
        super("Gestor de Archivos Blindados", false, false, false, false);
        this.userEmail = email;
        validarIntegridad();
        
        try {
            initComponentsCustom();
            applyPremiumStyle();
            setupSearch();
            cargarArchivosDesdeFirebase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validarIntegridad() {
        if (!SecuritySession.isIdentityValid(userEmail)) {
            AuditService.log(userEmail, "IDENTITY_VIOLATION_ARCHIVOS", "Intento de operación con identidad no válida.");
            JOptionPane.showMessageDialog(this, "🚨 ERROR DE SEGURIDAD:\nSu identidad digital no coincide con la sesión.", "Alerta", JOptionPane.ERROR_MESSAGE);
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
        txtRecordatorio.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtRuta.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtRuta.setEditable(false);
        
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar archivos por nombre o categoría...");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; margin: 5,15,5,15; background: #FFFFFF");

        btnSubir.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        
        String actionStyle = "arc: 14; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12; textAlign: left; margin: 0,20,0,20";
        btnEditar.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnDescargar.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, actionStyle + "; foreground: #D83B01; hoverBackground: #FDE7E9");
        
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnVolver.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; arc: 12; foreground: #0078D4; font: bold 11; background: #F3F9FF");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12; margin: 0,15,0,15");

        try {
            btnSubir.setIcon(new FlatSVGIcon("img/files.svg", 20, 20));
            btnDescargar.setIcon(new FlatSVGIcon("img/unlock.svg", 18, 18));
            btnEditar.setIcon(new FlatSVGIcon("img/palette.svg", 18, 18));
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
        lblTitulo = new JLabel("GESTOR DE ARCHIVOS RSA CRIPTOGRÁFICOS");
        panelLeftHeader.add(lblTitulo);
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);
        
        JPanel panelRightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        panelRightHeader.setOpaque(false);
        btnRefresh = new JButton("REFRESCAR LISTA");
        btnRefresh.addActionListener(e -> cargarArchivosDesdeFirebase());
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

        JLabel lblSec1 = new JLabel("SUBIR ARCHIVO");
        lblSec1.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        panelForm.add(lblSec1, gbc);

        txtNombre = new JTextField(); txtNombre.setPreferredSize(new Dimension(100, 40));
        txtCarpeta = new JTextField(); txtCarpeta.setPreferredSize(new Dimension(100, 40));
        txtRecordatorio = new JTextField(); txtRecordatorio.setPreferredSize(new Dimension(100, 40));
        txtRuta = new JTextField(); txtRuta.setPreferredSize(new Dimension(100, 40));
        
        addLabeledField(panelForm, "Nombre Descriptivo:", txtNombre, gbc, row++); row++;
        addLabeledField(panelForm, "Categoría / Carpeta:", txtCarpeta, gbc, row++); row++;
        addLabeledField(panelForm, "Nota:", txtRecordatorio, gbc, row++); row++;

        JLabel la = new JLabel("Archivo:"); la.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.insets = new Insets(5, 0, 2, 0); gbc.gridy = row++;
        panelForm.add(la, gbc);
        
        JPanel fileSelectPanel = new JPanel(new BorderLayout(5, 0));
        fileSelectPanel.setOpaque(false);
        fileSelectPanel.add(txtRuta, BorderLayout.CENTER);
        btnSeleccionar = new JButton("...");
        btnSeleccionar.setPreferredSize(new Dimension(45, 40));
        btnSeleccionar.addActionListener(e -> seleccionarArchivo());
        fileSelectPanel.add(btnSeleccionar, BorderLayout.EAST);
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        panelForm.add(fileSelectPanel, gbc);

        btnSubir = new JButton("SUBIR Y CIFRAR");
        btnSubir.setPreferredSize(new Dimension(100, 45));
        btnSubir.addActionListener(e -> subirArchivo());
        gbc.insets = new Insets(10, 0, 30, 0); gbc.gridy = row++;
        panelForm.add(btnSubir, gbc);

        btnCancelar = new JButton("CANCELAR EDICIÓN");
        btnCancelar.setVisible(false);
        btnCancelar.addActionListener(e -> resetForm());
        gbc.gridy = row++;
        panelForm.add(btnCancelar, gbc);

        JLabel lblSec2 = new JLabel("OPERACIONES");
        lblSec2.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(10, 0, 15, 0); gbc.gridy = row++;
        panelForm.add(lblSec2, gbc);

        btnEditar = new JButton("EDITAR METADATOS");
        btnEditar.addActionListener(e -> editarMetadata());
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        panelForm.add(btnEditar, gbc);

        btnDescargar = new JButton("DESCARGAR Y LIBERAR");
        btnDescargar.addActionListener(e -> descargarArchivo());
        gbc.gridy = row++;
        panelForm.add(btnDescargar, gbc);

        btnEliminar = new JButton("ELIMINAR DE LA NUBE");
        btnEliminar.addActionListener(e -> eliminarArchivo());
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

        model = new DefaultTableModel(new Object[]{"NOMBRE", "CATEGORÍA", "FECHA DE CARGA", "ID DDBB"}, 0) {
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
                if (text.trim().length() == 0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
    }

    private void cargarArchivosDesdeFirebase() {
        validarIntegridad();
        model.setRowCount(0);
        new Thread(() -> {
            try {
                Firestore db = FirebaseService.getFirestore();
                String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
                
                db.collection(path).get().get().getDocuments().forEach(doc -> {
                    String nombre = "N/A", carpeta = "N/A", fecha = "N/A";
                    try {
                        if (doc.contains("metadata_cifrada")) {
                            byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("metadata_cifrada")), SecuritySession.getPrivateKey());
                            String meta = new String(dec, StandardCharsets.UTF_8);
                            for (String p : meta.split("\\|")) {
                                if (p.startsWith("n:")) nombre = p.substring(2);
                                if (p.startsWith("c:")) carpeta = p.substring(2);
                            }
                        }
                        fecha = doc.getCreateTime() != null ? doc.getCreateTime().toDate().toString() : "N/A";
                    } catch (Exception e) { nombre = "[Error]"; }

                    String finalN = nombre, finalC = carpeta, finalF = fecha;
                    java.awt.EventQueue.invokeLater(() -> model.addRow(new Object[]{finalN, finalC, finalF, doc.getId()}));
                });
                
                java.awt.EventQueue.invokeLater(() -> {
                    table.getColumnModel().getColumn(3).setMinWidth(0);
                    table.getColumnModel().getColumn(3).setMaxWidth(0);
                });
            } catch (Exception e) {}
        }).start();
    }

    private void seleccionarArchivo() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fc.getSelectedFile();
            txtRuta.setText(archivoSeleccionado.getAbsolutePath());
            if (txtNombre.getText().isEmpty()) txtNombre.setText(archivoSeleccionado.getName());
        }
    }

    private void subirArchivo() {
        validarIntegridad();
        String nombre = txtNombre.getText().trim();
        String carpeta = txtCarpeta.getText().trim();
        if (nombre.isEmpty()) return;

        new Thread(() -> {
            try {
                Firestore db = FirebaseService.getFirestore();
                String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
                String metadata = "n:" + nombre + "|c:" + carpeta + "|r:" + txtRecordatorio.getText();
                byte[] encMeta = RSALogica.cifrarBytesHibrido(metadata.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                
                Map<String, Object> data = new HashMap<>();
                data.put("metadata_cifrada", Base64.getEncoder().encodeToString(encMeta));

                if (idDocumentoEdicion == null) {
                    if (archivoSeleccionado == null) return;
                    byte[] encBytes = RSALogica.cifrarBytesHibrido(Files.readAllBytes(archivoSeleccionado.toPath()), SecuritySession.getPublicKey());
                    data.put("contenido_cifrado", Base64.getEncoder().encodeToString(encBytes));
                    db.collection(path).add(data).get();
                } else {
                    db.collection(path).document(idDocumentoEdicion).update(data).get();
                }
                
                java.awt.EventQueue.invokeLater(() -> {
                    resetForm(); cargarArchivosDesdeFirebase();
                    ToastNotification.show(this, "Operación exitosa", ToastNotification.Type.SUCCESS);
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
            String docId = model.getValueAt(table.convertRowIndexToModel(fila), 3).toString();
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
            }
            idDocumentoEdicion = docId;
            btnSubir.setText("ACTUALIZAR");
            btnCancelar.setVisible(true);
            btnSeleccionar.setEnabled(false);
        } catch (Exception e) {}
    }

    private void descargarArchivo() {
        int fila = table.getSelectedRow();
        if (fila == -1) return;
        JPasswordField pf = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"PIN de seguridad:", pf}, "Verificación", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (SecuritySession.verifyUserPin(pf.getPassword())) {
                new Thread(() -> {
                    try {
                        String docId = model.getValueAt(table.convertRowIndexToModel(fila), 3).toString();
                        String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
                        var doc = FirebaseService.getFirestore().collection(path).document(docId).get().get();
                        byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(doc.getString("contenido_cifrado")), SecuritySession.getPrivateKey());
                        
                        java.awt.EventQueue.invokeLater(() -> {
                            JFileChooser fc = new JFileChooser();
                            fc.setSelectedFile(new File(model.getValueAt(fila, 0).toString()));
                            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                                try {
                                    Files.write(fc.getSelectedFile().toPath(), dec);
                                    ToastNotification.show(this, "Archivo recuperado", ToastNotification.Type.SUCCESS);
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            } else {
                JOptionPane.showMessageDialog(this, "PIN incorrecto", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminarArchivo() {
        int fila = table.getSelectedRow();
        if (fila == -1 || JOptionPane.showConfirmDialog(this, "¿Eliminar permanentemente?") != JOptionPane.YES_OPTION) return;
        new Thread(() -> {
            try {
                String docId = model.getValueAt(table.convertRowIndexToModel(fila), 3).toString();
                String path = RSALogica.hashToHex("usuarios") + "/" + RSALogica.hashToHex(userEmail) + "/" + RSALogica.hashToHex("archivos");
                FirebaseService.getFirestore().collection(path).document(docId).delete().get();
                java.awt.EventQueue.invokeLater(() -> {
                    cargarArchivosDesdeFirebase();
                    resetForm();
                });
            } catch (Exception e) {}
        }).start();
    }

    private void resetForm() {
        idDocumentoEdicion = null;
        txtNombre.setText(""); txtCarpeta.setText(""); txtRecordatorio.setText(""); txtRuta.setText("");
        archivoSeleccionado = null;
        btnSubir.setText("SUBIR Y CIFRAR");
        btnCancelar.setVisible(false);
        btnSeleccionar.setEnabled(true);
    }
}

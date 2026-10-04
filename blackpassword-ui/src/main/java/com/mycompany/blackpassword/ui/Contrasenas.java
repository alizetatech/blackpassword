package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.IntrusionService;
import com.mycompany.blackpassword.services.QRCodeService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.crypto.VaultManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.prefs.Preferences;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Bóveda de Contraseñas v16.5 - LUXURY COMMAND CENTER.
 * Diseño lateral optimizado con acceso total e ilimitado.
 */
public class Contrasenas extends javax.swing.JInternalFrame {

    private final String userEmail;
    private DefaultTableModel model;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    
    private JTextField txtService;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtSearch;
    
    private JButton btnSave;
    private JButton btnView;
    private JButton btnCopy;
    private JButton btnEdit;
    private JButton btnQR;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnVolver;
    private JButton btnGenPass;
    private JButton btnCopyUser;
    private JButton btnExportData;
    private JButton btnImportData;
    private JButton btnTogglePrivacy;
    private boolean privacyModeActive = false;
    private static final Preferences prefs = Preferences.userNodeForPackage(Contrasenas.class);
    
    private JLabel jLabelTitulo;
    private List<Map<String, Object>> secretDocumentsRaw = new ArrayList<>();

    public Contrasenas(String email) {
        super("Bóveda de Seguridad", false, false, false, false);
        this.userEmail = email;
        this.privacyModeActive = prefs.getBoolean("privacy_mode_" + userEmail, false);
        validarIntegridad();
        
        try {
            initComponentsCustom();
            applyPremiumStyle();
            setupSearch();
            setupSelectionLogic();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadSecretsFromFirebase();
    }

    private void validarIntegridad() {
        if (!SecuritySession.isIdentityValid(userEmail)) {
            AuditService.log(userEmail, "IDENTITY_VIOLATION_VAULT", "Intento de acceso a bóveda con identidad no válida.");
            JOptionPane.showMessageDialog(this, 
                "⚠️ ALERTA DE IDENTIDAD:\nSu identidad digital no está sincronizada correctamente.\n" +
                "Esto puede deberse a una actualización reciente del sistema.\n\n" +
                "Por favor, cierre sesión y vuelva a entrar para sincronizar sus huellas.", 
                "Sincronización Requerida", JOptionPane.WARNING_MESSAGE);
            
            // Usamos invokeLater para asegurar que el dispose ocurra después de mostrar el mensaje
            SwingUtilities.invokeLater(this::dispose);
        }
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        
        jLabelTitulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 22; foreground: #0078D4");
        
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight: 48;"
                + "showHorizontalLines: true;"
                + "intercellSpacing: 0,1;"
                + "selectionBackground: #E5F3FF;"
                + "selectionForeground: #000000;"
                + "showVerticalLines: false");
        
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold; background: #F8F9FA; separatorColor: #E0E0E0");
        
        String fieldStyle = "arc: 12; margin: 6,12,6,12; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4";
        txtService.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtUsername.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtPassword.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Buscar credencial por sitio o usuario...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 16; margin: 5,15,5,15; background: #FFFFFF");

        btnSave.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #107C10; foreground: #FFFFFF; hoverBackground: #0E6A0E; borderWidth: 0; font: bold 14");
        
        String actionStyle = "arc: 14; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12; horizontalAlignment: left; margin: 0,20,0,20";
        btnView.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnCopy.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnEdit.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnQR.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnDelete.putClientProperty(FlatClientProperties.STYLE, actionStyle + "; foreground: #D83B01; hoverBackground: #FDE7E9");
        
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12; margin: 0,15,0,15");
        btnGenPass.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnCopyUser.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #666666");

        btnVolver.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; arc: 12; foreground: #0078D4; font: bold 11; background: #F3F9FF");

        try {
            btnSave.setIcon(new FlatSVGIcon("img/vault.svg", 20, 20));
            btnView.setIcon(new FlatSVGIcon("img/unlock.svg", 18, 18));
            btnCopy.setIcon(new FlatSVGIcon("img/files.svg", 18, 18));
            btnEdit.setIcon(new FlatSVGIcon("img/key.svg", 18, 18));
            btnQR.setIcon(new FlatSVGIcon("img/palette.svg", 18, 18)); 
            btnDelete.setIcon(new FlatSVGIcon("img/panic.svg", 18, 18));
            btnRefresh.setIcon(new FlatSVGIcon("img/files.svg", 18, 18));
            btnVolver.setIcon(new FlatSVGIcon("img/vault.svg", 18, 18));
            btnCopyUser.setIcon(new FlatSVGIcon("img/files.svg", 16, 16));
            btnExportData.setIcon(new FlatSVGIcon("img/report.svg", 18, 18));
            btnImportData.setIcon(new FlatSVGIcon("img/unlock.svg", 18, 18));
        } catch (Exception e) {}
    }

    private void setupSearch() {
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txtSearch.getText();
                if (text.trim().length() == 0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
    }

    private void setupSelectionLogic() {
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() != -1;
            btnView.setEnabled(selected);
            btnCopy.setEnabled(selected);
            btnEdit.setEnabled(selected);
            btnQR.setEnabled(selected);
            btnDelete.setEnabled(selected);
        });
        btnView.setEnabled(false); btnCopy.setEnabled(false); btnEdit.setEnabled(false); btnQR.setEnabled(false); btnDelete.setEnabled(false);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    editSecret();
                }
            }
        });
    }

    private void initComponentsCustom() {
        // Eliminamos setSize fijo para usar dimensionamiento dinámico
        getContentPane().setLayout(new BorderLayout(0, 0));

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
        jLabelTitulo = new JLabel("BÓVEDA DE SEGURIDAD ZERO-TRUST");
        panelLeftHeader.add(jLabelTitulo);
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);
        
        JPanel panelRightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        panelRightHeader.setOpaque(false);
        btnRefresh = new JButton("SINCRONIZAR NUBE");
        btnRefresh.addActionListener(e -> loadSecretsFromFirebase());
        panelRightHeader.add(btnRefresh);
        
        btnExportData = new JButton("EXPORTAR DATOS");
        btnExportData.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #666666; foreground: #FFFFFF; font: bold 12");
        btnExportData.addActionListener(e -> showExportMenu());
        panelRightHeader.add(btnExportData);
        
        btnImportData = new JButton("IMPORTAR");
        btnImportData.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnImportData.addActionListener(e -> showImportDialog());
        panelRightHeader.add(btnImportData);
        
        panelHeader.add(panelRightHeader, BorderLayout.EAST);
        
        getContentPane().add(panelHeader, BorderLayout.NORTH);

        // --- SIDE PANEL (WEST) ---
        JPanel panelSide = new JPanel(new BorderLayout());
        panelSide.setBackground(new Color(250, 250, 250));
        panelSide.setPreferredSize(new Dimension(280, 0)); // Reducido de 350 a 280 para maximizar tabla
        panelSide.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        JPanel panelContentSide = new JPanel(new GridBagLayout());
        panelContentSide.setOpaque(false);
        panelContentSide.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        JLabel lblSec1 = new JLabel("NUEVA CREDENCIAL");
        lblSec1.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        panelContentSide.add(lblSec1, gbc);

        txtService = new JTextField(); txtService.setPreferredSize(new Dimension(100, 40));
        txtUsername = new JTextField(); txtUsername.setPreferredSize(new Dimension(100, 40));
        txtPassword = new JPasswordField(); txtPassword.setPreferredSize(new Dimension(100, 40));
        
        addLabeledField(panelContentSide, "Sitio / Servicio:", txtService, gbc, row++);
        row++;

        // Usuario con botón de copiado rápido
        gbc.insets = new Insets(5, 0, 2, 0); gbc.gridy = row++;
        JLabel lu = new JLabel("Usuario / Email:"); lu.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelContentSide.add(lu, gbc);
        
        JPanel userPanel = new JPanel(new BorderLayout(5, 0));
        userPanel.setOpaque(false);
        userPanel.add(txtUsername, BorderLayout.CENTER);
        btnCopyUser = new JButton();
        btnCopyUser.setToolTipText("Copiar usuario");
        btnCopyUser.setPreferredSize(new Dimension(40, 40));
        btnCopyUser.addActionListener(e -> {
            StringSelection sel = new StringSelection(txtUsername.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            ToastNotification.show(this, "Usuario copiado", ToastNotification.Type.INFO);
        });
        userPanel.add(btnCopyUser, BorderLayout.EAST);
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        panelContentSide.add(userPanel, gbc);

        // Contraseña con botón GEN
        gbc.insets = new Insets(5, 0, 2, 0); gbc.gridy = row++;
        JLabel lp = new JLabel("Contraseña:"); lp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelContentSide.add(lp, gbc);
        
        JPanel passPanel = new JPanel(new BorderLayout(5, 0));
        passPanel.setOpaque(false);
        passPanel.add(txtPassword, BorderLayout.CENTER);
        btnGenPass = new JButton("GEN");
        btnGenPass.setPreferredSize(new Dimension(60, 40));
        btnGenPass.addActionListener(e -> openPasswordGenerator());
        passPanel.add(btnGenPass, BorderLayout.EAST);
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        panelContentSide.add(passPanel, gbc);

        btnSave = new JButton("GUARDAR EN BÓVEDA");
        btnSave.setPreferredSize(new Dimension(100, 45));
        btnSave.addActionListener(e -> saveSecret());
        gbc.insets = new Insets(10, 0, 30, 0); gbc.gridy = row++;
        panelContentSide.add(btnSave, gbc);

        JLabel lblSec2 = new JLabel("ACCIONES DE REGISTRO");
        lblSec2.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #666666");
        gbc.insets = new Insets(10, 0, 15, 0); gbc.gridy = row++;
        panelContentSide.add(lblSec2, gbc);

        btnView = new JButton("VER CONTRASEÑA");
        btnView.addActionListener(e -> viewSecret());
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        panelContentSide.add(btnView, gbc);

        btnCopy = new JButton("COPIAR AL PORTAPAPELES");
        btnCopy.addActionListener(e -> copySecret());
        gbc.gridy = row++;
        panelContentSide.add(btnCopy, gbc);

        btnEdit = new JButton("EDITAR CREDENCIAL");
        btnEdit.addActionListener(e -> editSecret());
        gbc.gridy = row++;
        panelContentSide.add(btnEdit, gbc);

        btnQR = new JButton("GENERAR CÓDIGO QR");
        btnQR.addActionListener(e -> showQRCode());
        gbc.gridy = row++;
        panelContentSide.add(btnQR, gbc);

        btnDelete = new JButton("ELIMINAR PERMANENTEMENTE");
        btnDelete.addActionListener(e -> deleteSecret());
        gbc.insets = new Insets(20, 0, 0, 0); gbc.gridy = row++;
        panelContentSide.add(btnDelete, gbc);

        gbc.weighty = 1.0; gbc.gridy = row++;
        panelContentSide.add(new JLabel(""), gbc);

        JScrollPane sideScroll = new JScrollPane(panelContentSide);
        sideScroll.setBorder(null);
        sideScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        
        panelSide.add(sideScroll, BorderLayout.CENTER);
        getContentPane().add(panelSide, BorderLayout.WEST);

        // --- CENTER PANEL (TABLE) ---
        JPanel panelCenter = new JPanel(new BorderLayout(0, 15));
        panelCenter.setOpaque(false);
        panelCenter.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel panelSearch = new JPanel(new BorderLayout(15, 0));
        panelSearch.setOpaque(false);
        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(100, 42));
        panelSearch.add(txtSearch, BorderLayout.CENTER);

        btnTogglePrivacy = new JButton();
        btnTogglePrivacy.setPreferredSize(new Dimension(210, 42));
        btnTogglePrivacy.addActionListener(e -> {
            privacyModeActive = !privacyModeActive;
            prefs.putBoolean("privacy_mode_" + userEmail, privacyModeActive);
            updatePrivacyButton();
            table.repaint();
            ToastNotification.show(this, 
                privacyModeActive ? "Modo Privacidad activado (Identidades ocultas)" : "Modo Privacidad desactivado (Identidades visibles)", 
                ToastNotification.Type.INFO);
        });
        updatePrivacyButton();
        panelSearch.add(btnTogglePrivacy, BorderLayout.EAST);
        panelCenter.add(panelSearch, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"SITIO / SERVICIO", "USUARIO / IDENTIDAD", "ÚLTIMA ACTUALIZACIÓN"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        // Renderizador con máscara para Modo Privacidad (Censura de Usuario como contraseña para grabaciones)
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (privacyModeActive && value != null && !value.toString().trim().isEmpty() && !value.toString().equals("N/A") && !value.toString().startsWith("⚠️")) {
                    int len = Math.min(Math.max(value.toString().length(), 8), 16);
                    c.setText("•".repeat(len));
                    c.setToolTipText("Identidad protegida por Modo Privacidad");
                } else {
                    c.setText(value != null ? value.toString() : "");
                    c.setToolTipText(value != null ? value.toString() : null);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        panelCenter.add(scroll, BorderLayout.CENTER);
        
        getContentPane().add(panelCenter, BorderLayout.CENTER);
    }

    private void updatePrivacyButton() {
        if (privacyModeActive) {
            btnTogglePrivacy.setText("USUARIOS: OCULTOS");
            btnTogglePrivacy.setToolTipText("Modo Privacidad Activo: Los usuarios/emails se muestran censurados como contraseñas para grabaciones. Clic para mostrar.");
            btnTogglePrivacy.putClientProperty(FlatClientProperties.STYLE, 
                "arc: 14; background: #107C10; foreground: #FFFFFF; hoverBackground: #0E680E; font: bold 12");
            try {
                btnTogglePrivacy.setIcon(new FlatSVGIcon("img/shield.svg", 18, 18));
            } catch (Exception ignored) {}
        } else {
            btnTogglePrivacy.setText("USUARIOS: VISIBLES");
            btnTogglePrivacy.setToolTipText("Modo Privacidad Inactivo: Clic para ocultar los usuarios/emails en pantalla.");
            btnTogglePrivacy.putClientProperty(FlatClientProperties.STYLE, 
                "arc: 14; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; font: bold 12");
            try {
                btnTogglePrivacy.setIcon(new FlatSVGIcon("img/unlock.svg", 18, 18));
            } catch (Exception ignored) {}
        }
    }

    private void addLabeledField(JPanel p, String label, JTextField f, GridBagConstraints gbc, int row) {
        gbc.insets = new Insets(5, 0, 2, 0);
        gbc.gridy = row;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l, gbc);
        
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.gridy = row + 1;
        p.add(f, gbc);
    }

    private void openPasswordGenerator() {
        PasswordGeneratorPro gen = new PasswordGeneratorPro(SwingUtilities.getWindowAncestor(this));
        gen.setVisible(true);
        String pass = gen.getGeneratedPassword();
        if (pass != null) {
            txtPassword.setText(pass);
            ToastNotification.show(this, "Contraseña de alta seguridad generada.", ToastNotification.Type.SUCCESS);
        }
    }

    private void loadSecretsFromFirebase() {
        validarIntegridad();
        ToastNotification.show(this, "Sincronizando Bóveda...", ToastNotification.Type.INFO);
        model.setRowCount(0);
        
        VaultManager.getAllSecrets(userEmail).thenAccept(secrets -> {
            java.awt.EventQueue.invokeLater(() -> {
                secretDocumentsRaw.clear();
                secretDocumentsRaw.addAll(secrets);
                for (Map<String, Object> secret : secrets) {
                    model.addRow(new Object[]{
                        secret.getOrDefault("service", "N/A"),
                        secret.getOrDefault("username", "N/A"),
                        secret.getOrDefault("date", "N/A")
                    });
                }
                ToastNotification.show(this, "Sincronización completa.", ToastNotification.Type.SUCCESS);
            });
        }).exceptionally(ex -> {
            java.awt.EventQueue.invokeLater(() -> {
                ToastNotification.show(this, "Error de sincronización", ToastNotification.Type.ERROR);
            });
            return null;
        });
    }

    private void saveSecret() {
        validarIntegridad();
        String s = txtService.getText().trim();
        String u = txtUsername.getText().trim();
        char[] p = txtPassword.getPassword();
        
        if (s.isEmpty() || u.isEmpty() || p.length == 0) {
            ToastNotification.show(this, "Complete todos los campos.", ToastNotification.Type.WARNING);
            return;
        }

        VaultManager.saveSecret(userEmail, s, u, p).thenAccept(success -> {
            java.awt.EventQueue.invokeLater(() -> {
                if (success) {
                    ToastNotification.show(this, "Credencial guardada", ToastNotification.Type.SUCCESS);
                    AuditService.log(userEmail, "VAULT_ADD", "Sitio: " + s);
                    txtService.setText(""); txtUsername.setText(""); txtPassword.setText("");
                    loadSecretsFromFirebase();
                } else {
                    ToastNotification.show(this, "Error al guardar", ToastNotification.Type.ERROR);
                }
            });
        });
    }

    private void viewSecret() {
        validarIntegridad();
        int row = table.getSelectedRow();
        if (row == -1) return;
        JPasswordField pfPin = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"PIN de seguridad:", pfPin}, "Verificación", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (SecuritySession.verifyUserPin(pfPin.getPassword())) {
                IntrusionService.reportSuccess(userEmail);
                try {
                    String pass = decryptPasswordFromRow(row);
                    JPasswordField pfPass = new JPasswordField(pass); pfPass.setEditable(false);
                    pfPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
                    JOptionPane.showMessageDialog(this, pfPass, "CONTRASEÑA: " + model.getValueAt(row, 0), JOptionPane.INFORMATION_MESSAGE);
                    AuditService.log(userEmail, "VAULT_VIEW", "Acceso a: " + model.getValueAt(row, 0));
                } catch (Exception e) { ToastNotification.show(this, "Error al descifrar", ToastNotification.Type.ERROR); }
            } else {
                IntrusionService.reportFailure(userEmail, "VAULT_PIN_VIEW");
                JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copySecret() {
        validarIntegridad();
        int row = table.getSelectedRow();
        if (row == -1) return;
        JPasswordField pfPin = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"PIN de seguridad:", pfPin}, "Verificación", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (SecuritySession.verifyUserPin(pfPin.getPassword())) {
                IntrusionService.reportSuccess(userEmail);
                try {
                    String pass = decryptPasswordFromRow(row);
                    StringSelection selection = new StringSelection(pass);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    ToastNotification.show(this, "Copiado al portapapeles (expira en 30s)", ToastNotification.Type.SUCCESS);
                    AuditService.log(userEmail, "VAULT_PIN_COPY", "Copiado: " + model.getValueAt(row, 0));

                    // Autolimpiado del portapapeles tras 30 segundos
                    final String copiedSecret = pass;
                    javax.swing.Timer purgeTimer = new javax.swing.Timer(30000, evt -> {
                        try {
                            java.awt.datatransfer.Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                            if (clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                                String currentContent = (String) clip.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                                if (copiedSecret.equals(currentContent)) {
                                    clip.setContents(new StringSelection(""), null);
                                    ToastNotification.show(this, "Portapapeles limpiado por seguridad", ToastNotification.Type.INFO);
                                }
                            }
                        } catch (Exception ex) {}
                    });
                    purgeTimer.setRepeats(false);
                    purgeTimer.start();
                } catch (Exception e) { ToastNotification.show(this, "Error al descifrar", ToastNotification.Type.ERROR); }
            } else {
                IntrusionService.reportFailure(userEmail, "VAULT_PIN_COPY");
                JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSecret() {
        validarIntegridad();
        int row = table.getSelectedRow();
        if (row == -1) return;

        JPasswordField pfPin = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"PIN de seguridad:", pfPin}, "Verificación", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        if (!SecuritySession.verifyUserPin(pfPin.getPassword())) {
            IntrusionService.reportFailure(userEmail, "VAULT_PIN_EDIT");
            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        IntrusionService.reportSuccess(userEmail);

        int modelRow = table.convertRowIndexToModel(row);
        Map<String, Object> data = secretDocumentsRaw.get(modelRow);
        String oldService = (String) data.getOrDefault("service", "");
        String oldUser = (String) data.getOrDefault("username", "");
        String firestoreDocId = (String) data.get("id");

        String currentPass;
        try {
            currentPass = decryptPasswordFromRow(row);
        } catch (Exception e) {
            ToastNotification.show(this, "Error al descifrar para editar", ToastNotification.Type.ERROR);
            return;
        }

        JDialog editDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "EDITAR CREDENCIAL: " + oldService, JDialog.ModalityType.APPLICATION_MODAL);
        editDialog.setLayout(new BorderLayout(15, 15));

        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int dRow = 0;

        JTextField txtEditService = new JTextField(oldService);
        txtEditService.setPreferredSize(new Dimension(320, 38));
        String fieldStyle = "arc: 12; margin: 4,10,4,10; background: #FFFFFF; borderWidth: 1; focusColor: #0078D4";
        txtEditService.putClientProperty(FlatClientProperties.STYLE, fieldStyle);

        JTextField txtEditUser = new JTextField(oldUser);
        txtEditUser.setPreferredSize(new Dimension(320, 38));
        txtEditUser.putClientProperty(FlatClientProperties.STYLE, fieldStyle);

        JPasswordField txtEditPass = new JPasswordField(currentPass);
        txtEditPass.setPreferredSize(new Dimension(240, 38));
        txtEditPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");

        // Sitio / Servicio
        gbc.insets = new Insets(0, 0, 4, 0); gbc.gridy = dRow++;
        JLabel lblS = new JLabel("Sitio / Servicio:");
        lblS.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblS, gbc);
        gbc.insets = new Insets(0, 0, 12, 0); gbc.gridy = dRow++;
        formPanel.add(txtEditService, gbc);

        // Usuario / Email
        gbc.insets = new Insets(0, 0, 4, 0); gbc.gridy = dRow++;
        JLabel lblU = new JLabel("Usuario / Email:");
        lblU.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblU, gbc);
        gbc.insets = new Insets(0, 0, 12, 0); gbc.gridy = dRow++;
        formPanel.add(txtEditUser, gbc);

        // Contraseña
        gbc.insets = new Insets(0, 0, 4, 0); gbc.gridy = dRow++;
        JLabel lblP = new JLabel("Contraseña:");
        lblP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblP, gbc);

        JPanel passRowPanel = new JPanel(new BorderLayout(8, 0));
        passRowPanel.setOpaque(false);
        passRowPanel.add(txtEditPass, BorderLayout.CENTER);

        JButton btnGen = new JButton("GEN");
        btnGen.setPreferredSize(new Dimension(65, 38));
        btnGen.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #0078D4; font: bold");
        btnGen.addActionListener(e -> {
            PasswordGeneratorPro gen = new PasswordGeneratorPro(editDialog);
            gen.setVisible(true);
            String genPass = gen.getGeneratedPassword();
            if (genPass != null) {
                txtEditPass.setText(genPass);
            }
        });
        passRowPanel.add(btnGen, BorderLayout.EAST);

        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = dRow++;
        formPanel.add(passRowPanel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Botones de acción
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #F3F3F3; font: bold 12");
        btnCancel.addActionListener(e -> editDialog.dispose());

        JButton btnSaveEdit = new JButton("GUARDAR CAMBIOS");
        btnSaveEdit.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 12");
        btnSaveEdit.addActionListener(e -> {
            String newService = txtEditService.getText().trim();
            String newUser = txtEditUser.getText().trim();
            char[] newPass = txtEditPass.getPassword();

            if (newService.isEmpty() || newUser.isEmpty() || newPass.length == 0) {
                JOptionPane.showMessageDialog(editDialog, "Todos los campos son obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnSaveEdit.setEnabled(false);
            btnSaveEdit.setText("Guardando...");

            VaultManager.updateSecret(userEmail, firestoreDocId, oldService, newService, newUser, newPass).thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        ToastNotification.show(this, "Credencial actualizada exitosamente", ToastNotification.Type.SUCCESS);
                        AuditService.log(userEmail, "VAULT_EDIT", "Editado: " + oldService + " -> " + newService);
                        editDialog.dispose();
                        loadSecretsFromFirebase();
                    } else {
                        btnSaveEdit.setEnabled(true);
                        btnSaveEdit.setText("GUARDAR CAMBIOS");
                        JOptionPane.showMessageDialog(editDialog, "Error al actualizar la credencial en la bóveda.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSaveEdit);
        contentPanel.add(btnPanel, BorderLayout.SOUTH);

        editDialog.add(contentPanel, BorderLayout.CENTER);
        editDialog.pack();
        editDialog.setLocationRelativeTo(this);
        editDialog.setResizable(false);
        editDialog.setVisible(true);
    }

    private void showQRCode() {
        validarIntegridad();
        int row = table.getSelectedRow();
        if (row == -1) return;
        JPasswordField pfPin = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"PIN de seguridad:", pfPin}, "Verificación", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (SecuritySession.verifyUserPin(pfPin.getPassword())) {
                IntrusionService.reportSuccess(userEmail);
                try {
                    String pass = decryptPasswordFromRow(row);
                    BufferedImage qrImage = QRCodeService.generateQRCode(pass, 300, 300);
                    JDialog qrDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "QR TEMPORAL", JDialog.ModalityType.APPLICATION_MODAL);
                    qrDialog.setLayout(new BorderLayout(10, 10));
                    JLabel lblQR = new JLabel(new ImageIcon(qrImage));
                    lblQR.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
                    qrDialog.add(lblQR, BorderLayout.CENTER);
                    JLabel lblTimer = new JLabel("Autodestrucción en 15 segundos...", SwingConstants.CENTER);
                    lblTimer.setForeground(new Color(216, 59, 1)); lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    qrDialog.add(lblTimer, BorderLayout.SOUTH);
                    Timer timer = new Timer(1000, null); final int[] secondsLeft = {15};
                    timer.addActionListener(e -> {
                        secondsLeft[0]--;
                        if (secondsLeft[0] <= 0) qrDialog.dispose();
                        else lblTimer.setText("Autodestrucción en " + secondsLeft[0] + " segundos...");
                    });
                    qrDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosed(java.awt.event.WindowEvent e) {
                            timer.stop(); Graphics2D g = qrImage.createGraphics();
                            g.setColor(Color.BLACK); g.fillRect(0, 0, qrImage.getWidth(), qrImage.getHeight());
                            g.dispose(); qrImage.flush(); System.gc();
                        }
                    });
                    qrDialog.pack(); qrDialog.setLocationRelativeTo(this); timer.start(); qrDialog.setVisible(true);
                    AuditService.log(userEmail, "VAULT_QR_GEN", "QR generado para: " + model.getValueAt(row, 0));
                } catch (Exception e) { ToastNotification.show(this, "Error QR", ToastNotification.Type.ERROR); }
            } else {
                IntrusionService.reportFailure(userEmail, "VAULT_PIN_QR");
                JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String decryptPasswordFromRow(int row) throws Exception {
        Map<String, Object> data = secretDocumentsRaw.get(table.convertRowIndexToModel(row));
        String enc = (String) data.get("encrypted_payload");
        byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(enc), SecuritySession.getPrivateKey());
        String raw = new String(dec, StandardCharsets.UTF_8);
        for (String part : raw.split("\\|")) { if (part.startsWith("p:")) return part.substring(2); }
        return "";
    }

    private void deleteSecret() {
        validarIntegridad();
        int row = table.getSelectedRow();
        if (row == -1) return;
        
        int modelRow = table.convertRowIndexToModel(row);
        Map<String, Object> data = secretDocumentsRaw.get(modelRow);
        String serviceId = (String) data.get("service");
        String firestoreDocId = (String) data.get("id");
        
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar permanentemente " + serviceId + "?", "CONFIRMAR", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        
        VaultManager.deleteSecret(userEmail, serviceId, firestoreDocId);
        ToastNotification.show(this, "Eliminando...", ToastNotification.Type.SUCCESS);
        AuditService.log(userEmail, "VAULT_DELETE", "Eliminado: " + serviceId);
        loadSecretsFromFirebase();
    }

    private void showExportMenu() {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        
        javax.swing.JMenuItem itemPlain = new javax.swing.JMenuItem("Exportar en Plano (Excel)");
        itemPlain.addActionListener(e -> executeExport("PLAIN"));
        
        javax.swing.JMenuItem itemAes = new javax.swing.JMenuItem("Exportar con Cifrado AES-256");
        itemAes.addActionListener(e -> executeExport("AES"));
        
        javax.swing.JMenuItem itemKek = new javax.swing.JMenuItem("Exportar Blindado con KEK");
        itemKek.addActionListener(e -> executeExport("KEK"));
        
        menu.add(itemPlain);
        menu.add(itemAes);
        menu.add(itemKek);
        
        menu.show(btnExportData, 0, btnExportData.getHeight());
    }

    private void executeExport(String mode) {
        if (secretDocumentsRaw.isEmpty()) {
            ToastNotification.show(this, "Bóveda vacía, nada que exportar.", ToastNotification.Type.WARNING);
            return;
        }

        JPasswordField pfPin = new JPasswordField(4);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Ingrese PIN de seguridad para autorizar exportación:", pfPin}, "AUTORIZACIÓN REQUERIDA", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        
        if (!SecuritySession.verifyUserPin(pfPin.getPassword())) {
            JOptionPane.showMessageDialog(this, "PIN INCORRECTO. Exportación cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String csvContent = com.mycompany.blackpassword.services.ExportService.generateCsvContent(secretDocumentsRaw, SecuritySession.getPrivateKey());
            
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setDialogTitle("Guardar Reporte Bóveda (" + mode + ")");
            
            if (fc.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                File target = fc.getSelectedFile();
                char[] pass = null;

                switch (mode) {
                    case "PLAIN":
                        int confirmPlain = JOptionPane.showConfirmDialog(this, 
                            "⚠️ ADVERTENCIA DE SEGURIDAD CRÍTICA:\n\n" +
                            "Está a punto de exportar todas sus contraseñas en TEXTO PLANO (.csv).\n" +
                            "Cualquier persona o programa con acceso a este equipo podrá leerlas.\n\n" +
                            "¿Está absolutamente seguro de continuar?", 
                            "Riesgo de Exposición de Credenciales", 
                            JOptionPane.YES_NO_OPTION, 
                            JOptionPane.WARNING_MESSAGE);
                        if (confirmPlain != JOptionPane.YES_OPTION) return;

                        if (!target.getName().endsWith(".csv")) target = new File(target.getAbsolutePath() + ".csv");
                        Files.write(target.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
                        AuditService.log(userEmail, "CRITICAL_SECURITY_ALERT", "Exportación de bóveda en texto plano realizada: " + target.getName());
                        break;
                    case "AES":
                        if (!target.getName().endsWith(".enc")) target = new File(target.getAbsolutePath() + ".enc");
                        JPasswordField pfAes = new JPasswordField();
                        if (JOptionPane.showConfirmDialog(this, new Object[]{"Ingrese contraseña para el archivo AES:", pfAes}, "Cifrado AES", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                        pass = pfAes.getPassword();
                        com.mycompany.blackpassword.services.ExportService.exportAesEncrypted(csvContent, target, pass);
                        break;
                    case "KEK":
                        if (!target.getName().endsWith(".wcek")) target = new File(target.getAbsolutePath() + ".wcek");
                        JPasswordField pfKek = new JPasswordField();
                        if (JOptionPane.showConfirmDialog(this, new Object[]{"Ingrese Frase de Paso (Passphrase) para blindaje KEK:", pfKek}, "Blindaje KEK", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                        pass = pfKek.getPassword();
                        com.mycompany.blackpassword.services.ExportService.exportKekEncrypted(csvContent, target, pass);
                        break;
                }
                
                ToastNotification.show(this, "Reporte exportado con éxito.", ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "VAULT_EXPORT_" + mode, "Archivo: " + target.getName());
                if (pass != null) java.util.Arrays.fill(pass, '0');
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en exportación: " + e.getMessage(), "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showImportDialog() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Seleccionar archivo para importar a la Bóveda");
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            executeImport(fc.getSelectedFile());
        }
    }

    private void executeImport(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            String name = file.getName().toLowerCase();
            String content = "";
            char[] pass = null;

            if (name.endsWith(".csv")) {
                content = new String(data, StandardCharsets.UTF_8);
            } else if (name.endsWith(".enc")) {
                JPasswordField pfAes = new JPasswordField();
                if (JOptionPane.showConfirmDialog(this, new Object[]{"Archivo cifrado con AES. Ingrese contraseña:", pfAes}, "Desencriptación AES", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                pass = pfAes.getPassword();
                content = com.mycompany.blackpassword.services.ExportService.decryptAes(data, pass);
            } else if (name.endsWith(".wcek")) {
                JPasswordField pfKek = new JPasswordField();
                if (JOptionPane.showConfirmDialog(this, new Object[]{"Archivo blindado con KEK. Ingrese frase de paso:", pfKek}, "Desblindaje KEK", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                pass = pfKek.getPassword();
                content = com.mycompany.blackpassword.services.ExportService.decryptKek(data, pass);
            } else {
                throw new Exception("Formato no reconocido. Use .csv, .enc o .wcek");
            }

            List<Map<String, String>> allCredentials = com.mycompany.blackpassword.services.ExportService.parseCsv(content);
            if (allCredentials.isEmpty()) throw new Exception("No se encontraron registros válidos en el archivo.");

            // Abrir el diálogo interactivo de vista previa
            ImportPreviewDialog preview = new ImportPreviewDialog(javax.swing.SwingUtilities.getWindowAncestor(this), allCredentials);
            preview.setVisible(true);

            if (preview.isConfirmed()) {
                List<Map<String, String>> selectedCredentials = preview.getSelectedData();
                int count = 0;
                for (Map<String, String> cred : selectedCredentials) {
                    String s = cred.get("s");
                    String u = cred.get("u");
                    char[] p = cred.get("p").toCharArray();
                    VaultManager.saveSecret(userEmail, s, u, p);
                    count++;
                }

                ToastNotification.show(this, "Importación exitosa: " + count + " registros guardados.", ToastNotification.Type.SUCCESS);
                AuditService.log(userEmail, "VAULT_IMPORT_STAGED", "Importados " + count + " de " + allCredentials.size() + " desde " + name);
                loadSecretsFromFirebase();
            }

            if (pass != null) java.util.Arrays.fill(pass, '0');
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fallo en la importación: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

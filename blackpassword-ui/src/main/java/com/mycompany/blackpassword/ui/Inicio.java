package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.CifradorLogica;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.ConfigBundleService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.ProfileManager;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Ventana de Inicio v6.5-COMMAND-CENTER.
 * Gestión unificada de acceso con soporte para Paquete Maestro KEK autónomo
 * y modo tradicional multillave.
 */
public class Inicio extends javax.swing.JInternalFrame {

    public Inicio() {
        initComponents();
        applyPremiumStyle();
        cargarPerfiles();
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jLabelTitulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 24; foreground: #0078D4");
        
        lblVersion.setText("v6.5-COMMAND-CENTER");
        lblVersion.putClientProperty(FlatClientProperties.STYLE, "font: bold 10; foreground: #999999; background: #F8F9FA; border: 2,8,2,8, #F8F9FA, 1, 10");
        
        String fieldStyle = "arc: 12; background: #FFFFFF";
        comboPerfiles.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txrutaDatos.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txrutaPublica.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txrutaPrivada.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        jpassword.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        
        txrutaDatos.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Paquete Maestro (.kek) o Firebase (.black)");
        txrutaPublica.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Llave Pública (.pub) [Opcional]");
        txrutaPrivada.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Llave Privada (.key / .kek) [Opcional]");
        
        chkModoPaquete.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #0078D4");
        
        btdecifrar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        btnGeneradorConfig.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #107C10; foreground: #FFFFFF; hoverBackground: #0E680E; borderWidth: 0; font: bold 12");
        btnMigrarKek.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #6B21A8; foreground: #FFFFFF; hoverBackground: #581C87; borderWidth: 0; font: bold 12");
        btnGeneradorQr.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #E1F0FE; foreground: #0078D4; hoverBackground: #CCE5FF; borderWidth: 0; font: bold 12");
        
        jPanelMain.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 1,1,1,1, #E0E0E0, 1, 20");
    }

    private void cargarPerfiles() {
        List<ProfileManager.Profile> profiles = ProfileManager.getProfiles();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("-- Seleccionar Perfil --");
        for (ProfileManager.Profile p : profiles) model.addElement(p);
        comboPerfiles.setModel(model);
    }

    public void setBundlePath(String path) {
        chkModoPaquete.setSelected(true);
        toggleModoPaquete(true);
        txrutaDatos.setText(path);
        jpassword.requestFocusInWindow();
    }

    private void toggleModoPaquete(boolean esPaquete) {
        jLabelPub.setVisible(!esPaquete);
        txrutaPublica.setVisible(!esPaquete);
        btbuscarPub.setVisible(!esPaquete);
        jLabelPriv.setVisible(!esPaquete);
        txrutaPrivada.setVisible(!esPaquete);
        btbuscarPriv.setVisible(!esPaquete);
        if (esPaquete) {
            jLabelDatos.setText("Paquete Maestro de Configuración (.kek / .wfile):");
            txrutaDatos.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Archivo blackpassword_inicio.kek");
        } else {
            jLabelDatos.setText("Configuración de Firebase:");
            txrutaDatos.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Configuración Firebase (.black)");
        }
        revalidate();
        repaint();
        pack();
    }

    private void initComponents() {
        jPanelMain = new javax.swing.JPanel();
        jLabelTitulo = new javax.swing.JLabel();
        lblVersion = new javax.swing.JLabel();
        jLabelPerfil = new javax.swing.JLabel();
        comboPerfiles = new javax.swing.JComboBox<>();
        btnGuardarPerfil = new javax.swing.JButton();
        btnEliminarPerfil = new javax.swing.JButton();
        chkModoPaquete = new javax.swing.JCheckBox("📦 Modo Paquete Maestro KEK (1 solo archivo)", false);
        jLabelDatos = new javax.swing.JLabel();
        txrutaDatos = new javax.swing.JTextField();
        btbuscarDatos = new javax.swing.JButton();
        jLabelPub = new javax.swing.JLabel();
        txrutaPublica = new javax.swing.JTextField();
        btbuscarPub = new javax.swing.JButton();
        jLabelPriv = new javax.swing.JLabel();
        txrutaPrivada = new javax.swing.JTextField();
        btbuscarPriv = new javax.swing.JButton();
        jLabelPass = new javax.swing.JLabel();
        jpassword = new javax.swing.JPasswordField();
        btnVirtualKey = new javax.swing.JButton();
        btdecifrar = new javax.swing.JButton();
        btnGeneradorConfig = new javax.swing.JButton();
        btnMigrarKek = new javax.swing.JButton();
        btnGeneradorQr = new javax.swing.JButton();

        setClosable(true); setIconifiable(true); setTitle("🛡️ FORTALEZA v6.5");
        jPanelMain.setBackground(new java.awt.Color(255, 255, 255));
        jLabelTitulo.setHorizontalAlignment(SwingConstants.CENTER); jLabelTitulo.setText("Blackpassword Vault");
        lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabelPerfil.setText("Perfil de Usuario:");
        comboPerfiles.addActionListener(evt -> comboPerfilesActionPerformed(evt));
        btnGuardarPerfil.setText("💾"); btnGuardarPerfil.addActionListener(evt -> btnGuardarPerfilActionPerformed(evt));
        btnEliminarPerfil.setText("🗑️"); btnEliminarPerfil.addActionListener(evt -> btnEliminarPerfilActionPerformed(evt));
        
        chkModoPaquete.setOpaque(false);
        chkModoPaquete.addActionListener(evt -> toggleModoPaquete(chkModoPaquete.isSelected()));

        jLabelDatos.setText("Configuración de Firebase:");
        btbuscarDatos.setText("📂"); btbuscarDatos.addActionListener(evt -> seleccionarArchivo(txrutaDatos, "Configuración / Paquete"));
        jLabelPub.setText("Llave Pública RSA:");
        btbuscarPub.setText("📂"); btbuscarPub.addActionListener(evt -> seleccionarArchivo(txrutaPublica, "Llave Pública"));
        jLabelPriv.setText("Llave Privada RSA:");
        btbuscarPriv.setText("📂"); btbuscarPriv.addActionListener(evt -> seleccionarArchivo(txrutaPrivada, "Llave Privada"));
        jLabelPass.setText("Contraseña Maestra:");
        btnVirtualKey.setText("⌨️"); btnVirtualKey.addActionListener(evt -> VirtualKeyboard.show(javax.swing.SwingUtilities.getWindowAncestor(this), jpassword));
        
        btdecifrar.setText("DESCIFRAR Y CONECTAR");
        btdecifrar.addActionListener(evt -> btProcesarActionPerformed(evt));
        
        btnGeneradorConfig.setText("⚙️ GENERADOR DE CONFIGURACIÓN");
        btnGeneradorConfig.addActionListener(evt -> {
            javax.swing.JDesktopPane desktop = this.getDesktopPane();
            if (desktop != null) {
                for (javax.swing.JInternalFrame f : desktop.getAllFrames()) {
                    if (f instanceof GeneradorConfiguracionInicio && f.isVisible() && !f.isClosed()) {
                        try { if (f.isIcon()) f.setIcon(false); f.toFront(); f.setSelected(true); } catch (Exception ignored) {}
                        return;
                    }
                }
                GeneradorConfiguracionInicio genFrame = new GeneradorConfiguracionInicio(file -> {
                    setBundlePath(file.getAbsolutePath());
                });
                desktop.add(genFrame);
                int x = (desktop.getWidth() - genFrame.getWidth()) / 2;
                int y = (desktop.getHeight() - genFrame.getHeight()) / 2;
                genFrame.setLocation(Math.max(0, x), Math.max(0, y));
                genFrame.setVisible(true);
            }
        });

        btnMigrarKek.setText("🔄 ACTUALIZAR CONFIGURACIÓN DE .KEK");
        btnMigrarKek.addActionListener(evt -> {
            javax.swing.JDesktopPane desktop = this.getDesktopPane();
            if (desktop != null) {
                for (javax.swing.JInternalFrame f : desktop.getAllFrames()) {
                    if (f instanceof MigradorPaqueteKek && f.isVisible() && !f.isClosed()) {
                        try { if (f.isIcon()) f.setIcon(false); f.toFront(); f.setSelected(true); } catch (Exception ignored) {}
                        return;
                    }
                }
                MigradorPaqueteKek migradorFrame = new MigradorPaqueteKek(file -> {
                    setBundlePath(file.getAbsolutePath());
                });
                desktop.add(migradorFrame);
                int x = (desktop.getWidth() - migradorFrame.getWidth()) / 2;
                int y = (desktop.getHeight() - migradorFrame.getHeight()) / 2;
                migradorFrame.setLocation(Math.max(0, x), Math.max(0, y));
                migradorFrame.setVisible(true);
            }
        });

        btnGeneradorQr.setText("📱 HERRAMIENTA: GENERAR QR");
        btnGeneradorQr.addActionListener(evt -> {
            javax.swing.JDesktopPane desktop = this.getDesktopPane();
            if (desktop != null) {
                for (javax.swing.JInternalFrame f : desktop.getAllFrames()) {
                    if (f instanceof GeneradorQr && f.isVisible() && !f.isClosed()) {
                        try { if (f.isIcon()) f.setIcon(false); f.toFront(); f.setSelected(true); } catch (Exception ignored) {}
                        return;
                    }
                }
                GeneradorQr qrFrame = new GeneradorQr();
                desktop.add(qrFrame);
                int x = (desktop.getWidth() - qrFrame.getWidth()) / 2;
                int y = (desktop.getHeight() - qrFrame.getHeight()) / 2;
                qrFrame.setLocation(Math.max(0, x), Math.max(0, y));
                qrFrame.setVisible(true);
            }
        });

        javax.swing.GroupLayout jPanelMainLayout = new javax.swing.GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(jLabelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblVersion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabelPerfil)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(comboPerfiles, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnGuardarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEliminarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(chkModoPaquete)
                    .addComponent(jLabelDatos)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(txrutaDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btbuscarDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabelPub)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(txrutaPublica, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btbuscarPub, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabelPriv)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(txrutaPrivada, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btbuscarPriv, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabelPass)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(jpassword, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btdecifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGeneradorConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMigrarKek, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGeneradorQr, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelTitulo)
                    .addComponent(lblVersion))
                .addGap(18, 18, 18)
                .addComponent(jLabelPerfil)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboPerfiles, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(chkModoPaquete)
                .addGap(10, 10, 10)
                .addComponent(jLabelDatos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jLabelPub)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaPublica, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarPub, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jLabelPriv)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaPrivada, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarPriv, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addComponent(jLabelPass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jpassword, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addComponent(btdecifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnGeneradorConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnMigrarKek, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnGeneradorQr, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );
        getContentPane().add(jPanelMain); pack();
    }

    private void seleccionarArchivo(javax.swing.JTextField campo, String titulo) {
        JFileChooser selector = new JFileChooser(); selector.setDialogTitle(titulo);
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = selector.getSelectedFile().getAbsolutePath();
            campo.setText(path);
            if (campo == txrutaDatos && (path.toLowerCase().endsWith(".kek") || path.toLowerCase().endsWith(".wfile"))) {
                chkModoPaquete.setSelected(true);
                toggleModoPaquete(true);
            }
        }
    }

    private void comboPerfilesActionPerformed(java.awt.event.ActionEvent evt) {
        Object selected = comboPerfiles.getSelectedItem();
        if (selected instanceof ProfileManager.Profile p) {
            txrutaDatos.setText(p.firebasePath);
            txrutaPublica.setText(p.pubKeyPath);
            txrutaPrivada.setText(p.privKeyPath);
            boolean esPaquete = p.pubKeyPath == null || p.pubKeyPath.isEmpty();
            chkModoPaquete.setSelected(esPaquete);
            toggleModoPaquete(esPaquete);
        } else {
            txrutaDatos.setText(""); txrutaPublica.setText(""); txrutaPrivada.setText("");
        }
    }

    private void btnGuardarPerfilActionPerformed(java.awt.event.ActionEvent evt) {
        String rDatos = txrutaDatos.getText(), rPub = txrutaPublica.getText(), rPriv = txrutaPrivada.getText();
        if (rDatos.isEmpty()) return;
        if (!chkModoPaquete.isSelected() && (rPub.isEmpty() || rPriv.isEmpty())) return;
        String nombre = JOptionPane.showInputDialog(this, "Nombre del Perfil:");
        if (nombre != null && !nombre.trim().isEmpty()) {
            ProfileManager.saveProfile(new ProfileManager.Profile(nombre.trim(), rDatos, rPub, rPriv));
            cargarPerfiles();
        }
    }

    private void btnEliminarPerfilActionPerformed(java.awt.event.ActionEvent evt) {
        Object selected = comboPerfiles.getSelectedItem();
        if (selected instanceof ProfileManager.Profile p) {
            ProfileManager.deleteProfile(p.name); cargarPerfiles();
        }
    }

    private void btProcesarActionPerformed(java.awt.event.ActionEvent evt) {
        char[] pass = jpassword.getPassword();
        String rDatos = txrutaDatos.getText(), rPub = txrutaPublica.getText(), rPriv = txrutaPrivada.getText();
        
        if (pass.length == 0 || rDatos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete la ruta del archivo y la contraseña maestra.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean intentarPaquete = chkModoPaquete.isSelected() 
                || (rPub.isEmpty() && rPriv.isEmpty())
                || rDatos.toLowerCase().endsWith(".kek") 
                || rDatos.toLowerCase().endsWith(".wfile");

        try {
            File fDatos = new File(rDatos);
            if (!fDatos.exists()) {
                throw new Exception("El archivo no existe: " + rDatos);
            }

            if (intentarPaquete) {
                boolean exitoPaquete = false;
                try {
                    System.out.println("[v6.5] Intentando carga de Paquete Maestro KEK autónomo...");
                    ConfigBundleService.BundleResult bundle = ConfigBundleService.unpackBundle(fDatos, pass);

                    char[] sessionPinCopy = Arrays.copyOf(pass, pass.length);
                    SecuritySession.setKeySession(bundle.publicKey, bundle.privateKey, sessionPinCopy);

                    System.out.println("[v6.5] Conectando a Firebase desde Paquete KEK...");
                    FirebaseService.conectar(new String(bundle.firebaseConfigBytes, StandardCharsets.UTF_8));

                    Arrays.fill(bundle.firebaseConfigBytes, (byte) 0);
                    exitoPaquete = true;
                } catch (Exception bundleEx) {
                    if (rPub.isEmpty() || rPriv.isEmpty()) {
                        // Modo paquete era requerido y falló
                        throw bundleEx;
                    }
                    System.out.println("[v6.5] Archivo no es paquete KEK bundle, procediendo con modo tradicional: " + bundleEx.getMessage());
                }

                if (exitoPaquete) {
                    Arrays.fill(pass, (char) 0);
                    javax.swing.JDesktopPane desktop = this.getDesktopPane();
                    InicioSesion login = new InicioSesion();
                    desktop.add(login);
                    int x = (desktop.getWidth() - login.getWidth()) / 2, y = (desktop.getHeight() - login.getHeight()) / 2;
                    login.setLocation(x, y); login.setVisible(true);
                    this.dispose();
                    return;
                }
            }

            // Modo tradicional de 3 archivos
            if (rPub.isEmpty() || rPriv.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete todos los campos o active el modo Paquete Maestro.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            System.out.println("[v6.5] Cargando llaves RSA tradicionales...");
            PublicKey pub = RSALogica.cargarLlavePublica(new File(rPub), pass);
            PrivateKey priv;
            if (rPriv.toLowerCase().endsWith(".kek") || rPriv.toLowerCase().endsWith(".wfile")) {
                byte[] rawPriv = com.mycompany.blackpassword.services.ExportService.unblindFileToMemory(new File(rPriv), pass);
                priv = RSALogica.cargarLlavePrivadaDesdeBytes(rawPriv, new File(rPriv).getName(), pass, null);
            } else if (rPriv.toLowerCase().endsWith(".wcek")) {
                byte[] rawPriv = com.mycompany.blackpassword.services.ExportService.unblindWcekToMemory(new File(rPriv), pass);
                priv = RSALogica.cargarLlavePrivadaDesdeBytes(rawPriv, new File(rPriv).getName(), pass, null);
            } else {
                priv = RSALogica.cargarLlavePrivada(new File(rPriv), pass);
            }
            
            char[] sessionPinCopy = Arrays.copyOf(pass, pass.length);
            SecuritySession.setKeySession(pub, priv, sessionPinCopy);

            System.out.println("[v6.5] Descifrando configuracion Firebase...");
            byte[] decryptedJson;
            if (rDatos.toLowerCase().endsWith(".kek") || rDatos.toLowerCase().endsWith(".wfile")) {
                byte[] rawDatos = com.mycompany.blackpassword.services.ExportService.unblindFileToMemory(new File(rDatos), pass);
                decryptedJson = CifradorLogica.descifrarParaMemoriaRecursos(rawDatos, new File(rDatos).getName(), pass, SecuritySession.getUserSalt());
            } else if (rDatos.toLowerCase().endsWith(".wcek")) {
                byte[] rawDatos = com.mycompany.blackpassword.services.ExportService.unblindWcekToMemory(new File(rDatos), pass);
                decryptedJson = CifradorLogica.descifrarParaMemoriaRecursos(rawDatos, new File(rDatos).getName(), pass, SecuritySession.getUserSalt());
            } else {
                decryptedJson = CifradorLogica.descifrarParaMemoria(new File(rDatos), pass, SecuritySession.getUserSalt());
            }

            System.out.println("[v6.5] Conectando a Firebase...");
            FirebaseService.conectar(new String(decryptedJson, StandardCharsets.UTF_8));
            
            Arrays.fill(pass, (char) 0);
            Arrays.fill(decryptedJson, (byte) 0);
            
            javax.swing.JDesktopPane desktop = this.getDesktopPane();
            InicioSesion login = new InicioSesion();
            desktop.add(login);
            int x = (desktop.getWidth() - login.getWidth()) / 2, y = (desktop.getHeight() - login.getHeight()) / 2;
            login.setLocation(x, y); login.setVisible(true);
            this.dispose();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            Arrays.fill(pass, (char) 0);
            String errorMsg = ex.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) errorMsg = ex.toString();
            JOptionPane.showMessageDialog(this, "❌ FALLO DE ACCESO (v6.5):\n" + errorMsg, "Diagnóstico Blackpassword", JOptionPane.ERROR_MESSAGE);
        }
    }

    private javax.swing.JButton btbuscarDatos;
    private javax.swing.JButton btbuscarPub;
    private javax.swing.JButton btbuscarPriv;
    private javax.swing.JButton btdecifrar;
    private javax.swing.JButton btnGeneradorConfig;
    private javax.swing.JButton btnMigrarKek;
    private javax.swing.JButton btnGeneradorQr;
    private javax.swing.JButton btnGuardarPerfil;
    private javax.swing.JButton btnEliminarPerfil;
    private javax.swing.JButton btnVirtualKey;
    private javax.swing.JCheckBox chkModoPaquete;
    private javax.swing.JComboBox<Object> comboPerfiles;
    private javax.swing.JLabel jLabelDatos;
    private javax.swing.JLabel jLabelPub;
    private javax.swing.JLabel jLabelPriv;
    private javax.swing.JLabel jLabelPass;
    private javax.swing.JLabel jLabelPerfil;
    private javax.swing.JLabel jLabelTitulo;
    private javax.swing.JLabel lblVersion;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPasswordField jpassword;
    private javax.swing.JTextField txrutaDatos;
    private javax.swing.JTextField txrutaPublica;
    private javax.swing.JTextField txrutaPrivada;
}

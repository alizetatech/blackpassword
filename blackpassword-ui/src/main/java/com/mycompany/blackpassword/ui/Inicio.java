package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.CifradorLogica;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.ProfileManager;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import com.mycompany.blackpassword.services.ProfileManager;
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
 * Ventana de Inicio v6.2-PLATINUM.
 * Gestión avanzada de perfiles con estética Premium inyectada.
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
        
        txrutaDatos.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Configuración Firebase (.black)");
        txrutaPublica.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Llave Pública (.pub)");
        txrutaPrivada.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Llave Privada (.key)");
        
        btdecifrar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        
        jPanelMain.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 1,1,1,1, #E0E0E0, 1, 20");
    }

    private void cargarPerfiles() {
        List<ProfileManager.Profile> profiles = ProfileManager.getProfiles();
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("-- Seleccionar Perfil --");
        for (ProfileManager.Profile p : profiles) model.addElement(p);
        comboPerfiles.setModel(model);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanelMain = new javax.swing.JPanel();
        jLabelTitulo = new javax.swing.JLabel();
        lblVersion = new javax.swing.JLabel();
        jLabelPerfil = new javax.swing.JLabel();
        comboPerfiles = new javax.swing.JComboBox<>();
        btnGuardarPerfil = new javax.swing.JButton();
        btnEliminarPerfil = new javax.swing.JButton();
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

        setClosable(true); setIconifiable(true); setTitle("🛡️ FORTALEZA v6.2");
        jPanelMain.setBackground(new java.awt.Color(255, 255, 255));
        jLabelTitulo.setHorizontalAlignment(SwingConstants.CENTER); jLabelTitulo.setText("Blackpassword Vault");
        lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabelPerfil.setText("Perfil de Usuario:");
        comboPerfiles.addActionListener(evt -> comboPerfilesActionPerformed(evt));
        btnGuardarPerfil.setText("💾"); btnGuardarPerfil.addActionListener(evt -> btnGuardarPerfilActionPerformed(evt));
        btnEliminarPerfil.setText("🗑️"); btnEliminarPerfil.addActionListener(evt -> btnEliminarPerfilActionPerformed(evt));
        jLabelDatos.setText("Configuración de Firebase:");
        btbuscarDatos.setText("📂"); btbuscarDatos.addActionListener(evt -> seleccionarArchivo(txrutaDatos, "Configuración Firebase"));
        jLabelPub.setText("Llave Pública RSA:");
        btbuscarPub.setText("📂"); btbuscarPub.addActionListener(evt -> seleccionarArchivo(txrutaPublica, "Llave Pública"));
        jLabelPriv.setText("Llave Privada RSA:");
        btbuscarPriv.setText("📂"); btbuscarPriv.addActionListener(evt -> seleccionarArchivo(txrutaPrivada, "Llave Privada"));
        jLabelPass.setText("Contraseña Maestra:");
        btnVirtualKey.setText("⌨️"); btnVirtualKey.addActionListener(evt -> VirtualKeyboard.show(javax.swing.SwingUtilities.getWindowAncestor(this), jpassword));
        btdecifrar.setText("DESCIFRAR Y CONECTAR");
        btdecifrar.addActionListener(evt -> btProcesarActionPerformed(evt));

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
                    .addComponent(jLabelPass)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(jpassword, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btdecifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                        .addComponent(btbuscarPriv, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanelMainLayout.setVerticalGroup(
            jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelTitulo)
                    .addComponent(lblVersion))
                .addGap(20, 20, 20)
                .addComponent(jLabelPerfil)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboPerfiles, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEliminarPerfil, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabelDatos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarDatos, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabelPub)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaPublica, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarPub, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabelPriv)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txrutaPrivada, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btbuscarPriv, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(jLabelPass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jpassword, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(btdecifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );
        getContentPane().add(jPanelMain); pack();
    }

    private void seleccionarArchivo(javax.swing.JTextField campo, String titulo) {
        JFileChooser selector = new JFileChooser(); selector.setDialogTitle(titulo);
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) campo.setText(selector.getSelectedFile().getAbsolutePath());
    }

    private void comboPerfilesActionPerformed(java.awt.event.ActionEvent evt) {
        Object selected = comboPerfiles.getSelectedItem();
        if (selected instanceof ProfileManager.Profile p) {
            txrutaDatos.setText(p.firebasePath); txrutaPublica.setText(p.pubKeyPath); txrutaPrivada.setText(p.privKeyPath);
        } else {
            txrutaDatos.setText(""); txrutaPublica.setText(""); txrutaPrivada.setText("");
        }
    }

    private void btnGuardarPerfilActionPerformed(java.awt.event.ActionEvent evt) {
        String rDatos = txrutaDatos.getText(), rPub = txrutaPublica.getText(), rPriv = txrutaPrivada.getText();
        if (rDatos.isEmpty() || rPub.isEmpty() || rPriv.isEmpty()) return;
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
        
        if (pass.length == 0 || rDatos.isEmpty() || rPub.isEmpty() || rPriv.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            System.out.println("[v6.2] Cargando llaves RSA...");
            PublicKey pub = RSALogica.cargarLlavePublica(new File(rPub), pass);
            PrivateKey priv = RSALogica.cargarLlavePrivada(new File(rPriv), pass);
            
            SecuritySession.setKeySession(pub, priv, "0000".toCharArray());

            System.out.println("[v6.2] Descifrando configuracion Firebase...");
            byte[] decryptedJson = CifradorLogica.descifrarParaMemoria(new File(rDatos), pass);

            System.out.println("[v6.2] Conectando a Firebase...");
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
            JOptionPane.showMessageDialog(this, "❌ FALLO DE ACCESO (v6.2):\n" + errorMsg, "Diagnóstico Blackpassword", JOptionPane.ERROR_MESSAGE);
        }
    }

    private javax.swing.JButton btbuscarDatos;
    private javax.swing.JButton btbuscarPub;
    private javax.swing.JButton btbuscarPriv;
    private javax.swing.JButton btdecifrar;
    private javax.swing.JButton btnGuardarPerfil;
    private javax.swing.JButton btnEliminarPerfil;
    private javax.swing.JButton btnVirtualKey;
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

package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import java.io.File;
import java.security.KeyPair;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Generador RSA con triple opción de exportación (Plano, AES, KEK) - Edición Staging.
 */
public class GeneradorLlavesRsa extends javax.swing.JInternalFrame {

    private KeyPair currentKeyPair;

    public GeneradorLlavesRsa() {
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jLabel3.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        jTextField1.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del archivo...");
        jPasswordField1.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 10");
        
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; font: bold");
        
        String actionStyle = "arc: 12; background: #F3F3F3; font: bold 11";
        btnExportPlain.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnExportAes.putClientProperty(FlatClientProperties.STYLE, actionStyle);
        btnExportKek.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #107C10; foreground: #FFFFFF; font: bold 11");
        
        lblFingerprint.putClientProperty(FlatClientProperties.STYLE, "font: monospaced -2; foreground: #666666");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");

        setExportButtonsEnabled(false);
    }

    private void setExportButtonsEnabled(boolean enabled) {
        btnExportPlain.setEnabled(enabled);
        btnExportAes.setEnabled(enabled);
        btnExportKek.setEnabled(enabled);
    }

    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        btnGenerar = new javax.swing.JButton();
        lblFingerprint = new javax.swing.JLabel();
        btnExportPlain = new javax.swing.JButton();
        btnExportAes = new javax.swing.JButton();
        btnExportKek = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setTitle("Generador RSA - Opciones de Blindaje");

        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel3.setText("Identidad Criptográfica");

        jLabel1.setText("Nombre del archivo:");
        jLabel2.setText("Frase de Paso KEK / Contraseña AES:");

        btnGenerar.setText("GENERAR LLAVES RSA");
        btnGenerar.addActionListener(evt -> generarLlavesAction());

        lblFingerprint.setHorizontalAlignment(SwingConstants.CENTER);
        lblFingerprint.setText("Fingerprint: (Sin generar)");

        btnExportPlain.setText("EXPORTAR PLANO");
        btnExportPlain.addActionListener(evt -> saveKeys(false, false));

        btnExportAes.setText("EXPORTAR AES");
        btnExportAes.addActionListener(evt -> saveKeys(true, false));

        btnExportKek.setText("BLINDAR CON KEK");
        btnExportKek.addActionListener(evt -> saveKeys(true, true));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1)
                    .addComponent(jLabel2)
                    .addComponent(jPasswordField1)
                    .addComponent(btnGenerar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblFingerprint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(btnExportPlain, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnExportAes, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnExportKek, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel3)
                .addGap(35, 35, 35)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(btnGenerar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblFingerprint)
                .addGap(25, 25, 25)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnExportPlain)
                    .addComponent(btnExportAes)
                    .addComponent(btnExportKek))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );

        pack();
    }

    private void generarLlavesAction() {
        try {
            currentKeyPair = RSALogica.generarLlaves();
            String fp = RSALogica.hashPublicKey(currentKeyPair.getPublic());
            lblFingerprint.setText("Fingerprint: " + fp.substring(0, 32) + "...");
            setExportButtonsEnabled(true);
            ToastNotification.show(this, "Par de llaves RSA generado.", ToastNotification.Type.SUCCESS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en generación: " + e.getMessage());
        }
    }

    private void saveKeys(boolean protect, boolean isKek) {
        if (currentKeyPair == null) return;
        String nombre = jTextField1.getText().trim();
        if (nombre.isEmpty()) nombre = "identidad_rsa";
        
        char[] pass = jPasswordField1.getPassword();
        if (protect && pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Se requiere frase de paso para esta opción.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File dir = fc.getSelectedFile();

            String ext = isKek ? ".wkey" : ".key";
            byte[] salt = SecuritySession.getUserSalt();

            // Guardamos la pública siempre en plano (es pública)
            RSALogica.guardarLlave(new File(dir, nombre + ".pub"), currentKeyPair.getPublic().getEncoded(), false, null, null);
            // Guardamos la privada con la protección elegida
            RSALogica.guardarLlave(new File(dir, nombre + ext), currentKeyPair.getPrivate().getEncoded(), protect, pass, salt);

            JOptionPane.showMessageDialog(this, "Llaves exportadas con éxito.");
            if (isKek || protect) Arrays.fill(pass, (char) 0);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    private javax.swing.JButton btnGenerar;
    private javax.swing.JButton btnExportPlain;
    private javax.swing.JButton btnExportAes;
    private javax.swing.JButton btnExportKek;
    private javax.swing.JLabel lblFingerprint;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JTextField jTextField1;
}

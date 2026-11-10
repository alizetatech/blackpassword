package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.crypto.CifradorLogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Cifrador de Archivos AES-256 v1.3.
 * Permite cifrar y descifrar archivos locales con una contraseña simétrica.
 * Incluye opción de destrucción segura del archivo original.
 */
public class CifradorArchivo extends javax.swing.JInternalFrame {

    private File archivoSeleccionado = null;
    private final String userEmail;

    public CifradorArchivo(String email) {
        this.userEmail = email;
        validarIntegridad();
        initComponents();
        applyPremiumStyle();
    }

    private void validarIntegridad() {
        if ("Invitado".equals(userEmail)) return;
        if (!SecuritySession.isIdentityValid(userEmail)) {
            AuditService.log(userEmail, "IDENTITY_VIOLATION_ENCRYPTOR", "Intento de uso del cifrador con identidad no válida.");
            JOptionPane.showMessageDialog(this, "🚨 ERROR DE INTEGRIDAD:\nSu identidad digital no coincide con la sesión.", "Violación de Seguridad", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        if ("Invitado".equals(userEmail)) {
            lblTitulo.setText("Cifrador Local (Invitado)");
            lblTitulo.setForeground(new java.awt.Color(102, 102, 102));
        }

        txtRuta.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione un archivo...");
        txtRuta.setEditable(false);
        
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña de cifrado");
        
        btnCifrar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #107C10; foreground: #FFFFFF; font: bold");
        btnDescifrar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #0078D4; foreground: #FFFFFF; font: bold");
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #F3F3F3");
        
        checkDestruir.putClientProperty(FlatClientProperties.STYLE, "foreground: #D83B01; font: bold");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        txtRuta = new javax.swing.JTextField();
        btnSeleccionar = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        checkDestruir = new javax.swing.JCheckBox();
        btnCifrar = new javax.swing.JButton();
        btnDescifrar = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        setClosable(true);
        setIconifiable(true);
        setTitle("Cifrador AES-256");

        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));

        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setText("Cifrador de Archivos");

        jLabel1.setText("Archivo:");

        btnSeleccionar.setText("📂");
        btnSeleccionar.addActionListener(evt -> btnSeleccionarActionPerformed(evt));

        jLabel2.setText("Contraseña:");

        checkDestruir.setText("Destruir archivo original tras cifrar");

        btnCifrar.setText("CIFRAR ARCHIVO");
        btnCifrar.addActionListener(evt -> btnCifrarActionPerformed(evt));

        btnDescifrar.setText("DESCIFRAR ARCHIVO");
        btnDescifrar.addActionListener(evt -> btnDescifrarActionPerformed(evt));

        jLabel3.setText("<html><center>Utiliza AES-256 GCM para proteger sus archivos locales.<br>¡No olvide su contraseña!</center></html>");
        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel3.setForeground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(txtRuta, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSeleccionar, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2)
                    .addComponent(txtPassword)
                    .addComponent(checkDestruir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDescifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(lblTitulo)
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtRuta, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSeleccionar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(checkDestruir)
                .addGap(18, 18, 18)
                .addComponent(btnCifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnDescifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(20, 20, 20))
        );

        pack();
    }

    private void btnSeleccionarActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fc.getSelectedFile();
            txtRuta.setText(archivoSeleccionado.getAbsolutePath());
        }
    }

    private void btnCifrarActionPerformed(java.awt.event.ActionEvent evt) {
        validarIntegridad();
        if (archivoSeleccionado == null || txtPassword.getPassword().length == 0) {
            ToastNotification.show(this, "Seleccione un archivo y una contraseña.", ToastNotification.Type.WARNING);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(archivoSeleccionado.getAbsolutePath() + ".black"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            char[] pass = txtPassword.getPassword();
            try {
                File original = archivoSeleccionado;
                CifradorLogica.cifrarArchivo(original, fc.getSelectedFile(), pass, SecuritySession.getUserSalt());
                
                if (checkDestruir.isSelected()) {
                    try {
                        long size = original.length();
                        byte[] zeros = new byte[8192];
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(original)) {
                            long written = 0;
                            while (written < size) {
                                int toWrite = (int) Math.min(zeros.length, size - written);
                                fos.write(zeros, 0, toWrite);
                                written += toWrite;
                            }
                            fos.getFD().sync();
                        }
                    } catch (Exception ignored) {}
                    if (original.delete()) {
                        ToastNotification.show(this, "Archivo cifrado y original destruido de forma segura.", ToastNotification.Type.SUCCESS);
                        archivoSeleccionado = null;
                        txtRuta.setText("");
                    } else {
                        ToastNotification.show(this, "Cifrado OK, pero no se pudo eliminar el original.", ToastNotification.Type.WARNING);
                    }
                } else {
                    ToastNotification.show(this, "Archivo cifrado con éxito.", ToastNotification.Type.SUCCESS);
                }

                if (!"Invitado".equals(userEmail)) {
                    AuditService.log(userEmail, "FILE_ENCRYPT_LOCAL", "Archivo: " + original.getName() + (checkDestruir.isSelected() ? " (DESTRUIDO)" : ""));
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al cifrar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                java.util.Arrays.fill(pass, (char) 0);
            }
        }
    }

    private void btnDescifrarActionPerformed(java.awt.event.ActionEvent evt) {
        validarIntegridad();
        if (archivoSeleccionado == null || txtPassword.getPassword().length == 0) {
            ToastNotification.show(this, "Seleccione un archivo y una contraseña.", ToastNotification.Type.WARNING);
            return;
        }

        JFileChooser fc = new JFileChooser();
        String originalName = archivoSeleccionado.getName().replace(".black", "");
        fc.setSelectedFile(new File(archivoSeleccionado.getParent(), "descifrado_" + originalName));
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            char[] pass = txtPassword.getPassword();
            try {
                CifradorLogica.descifrarArchivo(archivoSeleccionado, fc.getSelectedFile(), pass, SecuritySession.getUserSalt());
                ToastNotification.show(this, "Archivo descifrado con éxito.", ToastNotification.Type.SUCCESS);
                if (!"Invitado".equals(userEmail)) {
                    AuditService.log(userEmail, "FILE_DECRYPT_LOCAL", "Archivo: " + archivoSeleccionado.getName());
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al descifrar: Contraseña incorrecta o archivo corrupto.", "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                java.util.Arrays.fill(pass, (char) 0);
            }
        }
    }

    private javax.swing.JButton btnCifrar;
    private javax.swing.JButton btnDescifrar;
    private javax.swing.JButton btnSeleccionar;
    private javax.swing.JCheckBox checkDestruir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtRuta;
}

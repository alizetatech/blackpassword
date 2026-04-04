package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.RSALogica;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Ventana para descifrar archivos RSA usando la llave privada protegida.
 * Implementa purga de memoria para la contraseña de la llave.
 */
public class DescifradorRsa extends javax.swing.JInternalFrame {

    private File archivoRsa;
    private File archivoLlave;

    public DescifradorRsa() {
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        
        jLabel4.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        txtRsa.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Selecciona el archivo .rsa");
        txtKey.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Selecciona tu llave privada .key");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        txtPassword.setText("");
        
        btnDescifrar.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
            
        String btnSearchStyle = "arc: 8; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5";
        btnSelRsa.putClientProperty(FlatClientProperties.STYLE, btnSearchStyle);
        btnSelKey.putClientProperty(FlatClientProperties.STYLE, btnSearchStyle);
        
        // Redondeo aplicado a través del borde del panel
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanelCard = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        txtRsa = new javax.swing.JTextField();
        btnSelRsa = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        txtKey = new javax.swing.JTextField();
        btnSelKey = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        btnDescifrar = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setTitle("Descifrador RSA Premium");

        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel4.setText("Acceso con Llave Privada");

        jLabel1.setText("Archivo Cifrado (.rsa):");
        btnSelRsa.setText("📂");
        btnSelRsa.addActionListener(e -> seleccionarRsa());

        jLabel2.setText("Llave Privada (.key):");
        btnSelKey.setText("📂");
        btnSelKey.addActionListener(e -> seleccionarKey());

        jLabel3.setText("Contraseña de la Llave:");

        btnDescifrar.setText("DESCIFRAR Y VER CONTENIDO");
        btnDescifrar.addActionListener(e -> ejecutarDescifrado());

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(txtRsa, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSelRsa, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(txtKey, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSelKey, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(txtPassword)
                    .addComponent(btnDescifrar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel4)
                .addGap(30, 30, 30)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtRsa, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSelRsa, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtKey, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSelKey, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(btnDescifrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
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

    private void seleccionarRsa() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoRsa = fc.getSelectedFile();
            txtRsa.setText(archivoRsa.getAbsolutePath());
        }
    }

    private void seleccionarKey() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoLlave = fc.getSelectedFile();
            txtKey.setText(archivoLlave.getAbsolutePath());
        }
    }

    private void ejecutarDescifrado() {
        char[] pass = txtPassword.getPassword();
        if (archivoRsa == null || archivoLlave == null || pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Faltan datos para el descifrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            PrivateKey pk = RSALogica.cargarLlavePrivada(archivoLlave, pass);
            byte[] resultado = RSALogica.descifrarHibrido(archivoRsa, pk);
            
            JTextArea area = new JTextArea(15, 40);
            area.setText(new String(resultado, StandardCharsets.UTF_8));
            area.setEditable(false);
            area.putClientProperty(FlatClientProperties.STYLE, "font: monospaced");
            
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Contenido Descifrado", JOptionPane.PLAIN_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al descifrar: " + e.getMessage(), "Fallo de Seguridad", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pass, (char) 0);
        }
    }

    private javax.swing.JButton btnDescifrar;
    private javax.swing.JButton btnSelKey;
    private javax.swing.JButton btnSelRsa;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtKey;
    private javax.swing.JTextField txtRsa;
}

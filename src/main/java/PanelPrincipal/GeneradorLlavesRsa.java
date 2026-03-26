package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.RSALogica;
import java.io.File;
import java.security.KeyPair;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Generador de Seguridad RSA con protección AES en ambas llaves - Edición Premium.
 * Implementa purga de memoria para la contraseña de protección.
 */
public class GeneradorLlavesRsa extends javax.swing.JInternalFrame {

    public GeneradorLlavesRsa() {
        initComponents();
        applyPremiumStyle();
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        
        jLabel3.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        jTextField1.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del paquete (ej: mi_identidad)");
        jPasswordField1.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        jPasswordField1.setText("");
        
        jButton1.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
            
        // Redondeo aplicado a través del borde del panel
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanelCard = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setTitle("Generador RSA - Seguridad Total");

        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel3.setText("Identidad Criptográfica");

        jLabel1.setText("Nombre del archivo:");
        jLabel2.setText("Contraseña de protección (AES):");

        jButton1.setText("GENERAR LLAVES BLINDADAS");
        jButton1.addActionListener(evt -> jButton1ActionPerformed(evt));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1)
                    .addComponent(jLabel2)
                    .addComponent(jPasswordField1)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE))
                .addContainerGap(40, Short.MAX_VALUE))
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
                .addGap(35, 35, 35)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = jTextField1.getText().trim();
        char[] pass = jPasswordField1.getPassword();
        
        if (nombre.isEmpty() || pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Rellena todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Seleccionar carpeta de destino");
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File dir = fc.getSelectedFile();

            KeyPair llaves = RSALogica.generarLlaves();

            RSALogica.guardarLlave(new File(dir, nombre + ".pub"), llaves.getPublic().getEncoded(), true, pass);
            RSALogica.guardarLlave(new File(dir, nombre + ".key"), llaves.getPrivate().getEncoded(), true, pass);

            File rsaFile = new File(dir, nombre + ".rsa");
            RSALogica.cifrarContenidoHibrido("Contenido protegido por Blackpassword.".getBytes(), rsaFile, llaves.getPublic());

            JOptionPane.showMessageDialog(this, "¡ÉXITO! Ambas llaves han sido generadas y CIFRADAS con AES.", "Seguridad Total", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pass, (char) 0);
        }
    }

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JTextField jTextField1;
}

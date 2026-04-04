package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.PGPLogica;
import com.mycompany.blackpassword.crypto.PGPLogica.PGPKeyPairResult;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

/**
 * Módulo de Generación de Llaves OpenPGP - Edición Blindada.
 * Permite crear identidades criptográficas compatibles con GPG.
 */
public class GeneradorPgp extends javax.swing.JInternalFrame {

    public GeneradorPgp() {
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        
        jLabelTitle.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        jTextFieldIdentity.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: Juan Perez <juan@ejemplo.com>");
        jPasswordFieldPass.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 10");
        jPasswordFieldPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Opcional (Dejar vacío para sin contraseña)");
        
        jComboBoxSize.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        
        jButtonGenerate.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 20; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
            
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 25");
    }

    private void initComponents() {
        jPanelCard = new JPanel();
        jLabelTitle = new JLabel("Generador OpenPGP");
        jLabelIdent = new JLabel("Identidad del Usuario (UID):");
        jTextFieldIdentity = new JTextField();
        jLabelSize = new JLabel("Tamaño de Llave (RSA):");
        jComboBoxSize = new JComboBox<>(new String[]{"2048 bits (Estándar)", "4096 bits (Militar)"});
        jLabelPass = new JLabel("Contraseña de Privacidad (Passphrase):");
        jPasswordFieldPass = new JPasswordField();
        jButtonGenerate = new JButton("GENERAR IDENTIDAD PGP");

        setClosable(true);
        setIconifiable(true);
        setTitle("Seguridad PGP");

        jPanelCard.setBackground(Color.WHITE);

        jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);

        jButtonGenerate.addActionListener(evt -> handleGeneration());

        GroupLayout jPanelCardLayout = new GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelTitle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelIdent)
                    .addComponent(jTextFieldIdentity)
                    .addComponent(jLabelSize)
                    .addComponent(jComboBoxSize, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelPass)
                    .addComponent(jPasswordFieldPass)
                    .addComponent(jButtonGenerate, GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabelTitle)
                .addGap(30, 30, 30)
                .addComponent(jLabelIdent)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldIdentity, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabelSize)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxSize, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabelPass)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordFieldPass, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(jButtonGenerate, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );

        pack();
    }

    private void handleGeneration() {
        String identity = jTextFieldIdentity.getText().trim();
        char[] pass = jPasswordFieldPass.getPassword();
        int size = jComboBoxSize.getSelectedIndex() == 0 ? 2048 : 4096;

        if (identity.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese una identidad (Nombre <email@...)");
            return;
        }

        // Mostrar un diálogo de "Cargando" ya que PGP 4096 puede tardar
        Cursor cursorOriginal = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        jButtonGenerate.setEnabled(false);

        new Thread(() -> {
            try {
                PGPKeyPairResult result = PGPLogica.generatePGPKeyPair(identity, pass, size);
                
                SwingUtilities.invokeLater(() -> {
                    saveKeysToDisk(result, identity);
                    setCursor(cursorOriginal);
                    jButtonGenerate.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error en generación: " + e.getMessage(), "Error PGP", JOptionPane.ERROR_MESSAGE);
                    setCursor(cursorOriginal);
                    jButtonGenerate.setEnabled(true);
                });
            } finally {
                Arrays.fill(pass, '0');
            }
        }).start();
    }

    private void saveKeysToDisk(PGPKeyPairResult result, String identity) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Carpeta de Destino para el Par de Llaves PGP");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            String cleanName = identity.replaceAll("[^a-zA-Z0-9]", "_");
            
            try {
                File pubFile = new File(dir, cleanName + "_public.asc");
                File privFile = new File(dir, cleanName + "_private.asc");
                
                try (FileWriter fw = new FileWriter(pubFile)) { fw.write(result.publicKey); }
                try (FileWriter fw = new FileWriter(privFile)) { fw.write(result.privateKey); }
                
                JOptionPane.showMessageDialog(this, 
                    "¡Identidad PGP Generada con Éxito!\n\n" +
                    "Archivos creados:\n" +
                    "- " + pubFile.getName() + "\n" +
                    "- " + privFile.getName() + "\n\n" +
                    "Nota: La llave privada está protegida según tu elección.",
                    "Éxito Criptográfico", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivos: " + e.getMessage());
            }
        }
    }

    private JButton jButtonGenerate;
    private JComboBox<String> jComboBoxSize;
    private JLabel jLabelIdent, jLabelPass, jLabelSize, jLabelTitle;
    private JPanel jPanelCard;
    private JPasswordField jPasswordFieldPass;
    private JTextField jTextFieldIdentity;
}

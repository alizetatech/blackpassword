package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.services.ExportService;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

/**
 * Herramienta de Blindaje de Archivos KEK (File Blinder) v1.0.
 * Permite cifrar/descifrar archivos locales con una frase de paso.
 */
public class BlindadorArchivos extends javax.swing.JInternalFrame {

    private File selectedFile;

    public BlindadorArchivos() {
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
        
        jLabelTitle.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        txtPath.setEditable(false);
        txtPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione un archivo para blindar...");
        
        jPasswordField1.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 10");
        jPasswordField1.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Frase de Paso KEK...");

        btnBrowse.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #666666; foreground: #FFFFFF");
        
        btnBlindar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #107C10; foreground: #FFFFFF; font: bold");
        btnDesblindar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #0078D4; foreground: #FFFFFF; font: bold");
        
        jProgressBar1.setVisible(false);
        jProgressBar1.setStringPainted(true);
    }

    private void initComponents() {
        jPanelCard = new JPanel();
        jLabelTitle = new JLabel("Blindador de Archivos KEK");
        jLabel1 = new JLabel("Archivo de origen:");
        txtPath = new JTextField();
        btnBrowse = new JButton("BUSCAR");
        jLabel2 = new JLabel("Frase de Paso para el Blindaje:");
        jPasswordField1 = new JPasswordField();
        btnBlindar = new JButton("BLINDAR ARCHIVO");
        btnDesblindar = new JButton("DESBLINDAR ARCHIVO");
        jProgressBar1 = new JProgressBar();

        setClosable(true);
        setIconifiable(true);
        setTitle("Seguridad de Archivos");

        jPanelCard.setBackground(Color.WHITE);

        jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        btnBrowse.addActionListener(e -> browseAction());
        btnBlindar.addActionListener(e -> processAction(true));
        btnDesblindar.addActionListener(e -> processAction(false));

        GroupLayout jPanelCardLayout = new GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                    .addComponent(jProgressBar1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelTitle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(txtPath, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowse, GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE))
                    .addComponent(jLabel2)
                    .addComponent(jPasswordField1)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(btnBlindar, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnDesblindar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabelTitle)
                .addGap(30, 30, 30)
                .addComponent(jLabel1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPath, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowse, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addGroup(jPanelCardLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnBlindar, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDesblindar, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(jProgressBar1, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jPanelCard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(25, 25, 25))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jPanelCard, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(25, 25, 25))
        );

        pack();
    }

    private void browseAction() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            txtPath.setText(selectedFile.getAbsolutePath());
        }
    }

    private void processAction(boolean blindar) {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo primero.");
            return;
        }
        char[] pass = jPasswordField1.getPassword();
        if (pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Ingrese una Frase de Paso.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar destino");
        fc.setSelectedFile(new File(selectedFile.getParent(), 
            blindar ? selectedFile.getName() + ".wfile" : selectedFile.getName().replace(".wfile", "")));
        
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();

        jProgressBar1.setVisible(true);
        jProgressBar1.setValue(0);
        
        new Thread(() -> {
            try {
                if (blindar) {
                    ExportService.blindFile(selectedFile, target, pass, p -> SwingUtilities.invokeLater(() -> jProgressBar1.setValue((int)(p * 100))));
                } else {
                    ExportService.unblindFile(selectedFile, target, pass, p -> SwingUtilities.invokeLater(() -> jProgressBar1.setValue((int)(p * 100))));
                }
                
                SwingUtilities.invokeLater(() -> {
                    jProgressBar1.setValue(100);
                    JOptionPane.showMessageDialog(this, "Operación completada con éxito.", "Blindaje KEK", JOptionPane.INFORMATION_MESSAGE);
                    jProgressBar1.setVisible(false);
                    jPasswordField1.setText("");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
                    jProgressBar1.setVisible(false);
                });
            } finally {
                Arrays.fill(pass, '0');
            }
        }).start();
    }

    private JPanel jPanelCard;
    private JLabel jLabelTitle;
    private JLabel jLabel1;
    private JTextField txtPath;
    private JButton btnBrowse;
    private JLabel jLabel2;
    private JPasswordField jPasswordField1;
    private JButton btnBlindar;
    private JButton btnDesblindar;
    private JProgressBar jProgressBar1;
}

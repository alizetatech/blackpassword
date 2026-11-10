package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Generador de Llaves CEK (Content Encryption Key) - LUXURY EDITION.
 * Produce llaves simétricas AES-256 de alta entropía para cifrado de datos.
 */
public class GeneradorCek extends javax.swing.JInternalFrame {

    private byte[] currentCek = null;
    private JTextArea txtCekDisplay;
    private JButton btnGenerate, btnCopy, btnExport;
    private JLabel lblTitle, lblInfo;

    public GeneradorCek() {
        super("Generador de Llaves CEK", true, true, true, true);
        initComponentsCustom();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 22; foreground: #0078D4");
        
        txtCekDisplay.putClientProperty(FlatClientProperties.STYLE, ""
                + "font: monospaced +2;"
                + "background: #F8F9FA;"
                + "border: 10,10,10,10");
        txtCekDisplay.setEditable(false);
        txtCekDisplay.setLineWrap(true);

        btnGenerate.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 20; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        
        String secondaryBtnStyle = "arc: 16; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12";
        btnCopy.putClientProperty(FlatClientProperties.STYLE, secondaryBtnStyle);
        btnExport.putClientProperty(FlatClientProperties.STYLE, secondaryBtnStyle);
        
        try {
            btnGenerate.setIcon(new FlatSVGIcon("img/security.svg", 20, 20));
            btnCopy.setIcon(new FlatSVGIcon("img/report.svg", 18, 18));
            btnExport.setIcon(new FlatSVGIcon("img/files.svg", 18, 18));
        } catch (Exception e) {}
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        lblTitle = new JLabel("GENERADOR DE LLAVES CEK");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = row++;
        mainPanel.add(lblTitle, gbc);

        lblInfo = new JLabel("Genera llaves simétricas AES-256 para cifrado de archivos o comunicaciones.");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setForeground(new Color(100, 100, 100));
        gbc.insets = new Insets(0, 0, 30, 0); gbc.gridy = row++;
        mainPanel.add(lblInfo, gbc);

        txtCekDisplay = new JTextArea(4, 30);
        txtCekDisplay.setText("Haga clic en el botón para generar una nueva llave...");
        JScrollPane scroll = new JScrollPane(txtCekDisplay);
        scroll.setBorder(null);
        gbc.insets = new Insets(0, 0, 25, 0); gbc.gridy = row++;
        mainPanel.add(scroll, gbc);

        btnGenerate = new JButton("GENERAR NUEVA CEK (AES-256)");
        btnGenerate.setPreferredSize(new Dimension(100, 50));
        btnGenerate.addActionListener(e -> generateCek());
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        mainPanel.add(btnGenerate, gbc);

        JPanel buttonRow = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonRow.setOpaque(false);
        btnCopy = new JButton("COPIAR");
        btnCopy.setPreferredSize(new Dimension(80, 40));
        btnCopy.addActionListener(e -> copyToClipboard());
        
        btnExport = new JButton("EXPORTAR (.KEY)");
        btnExport.addActionListener(e -> exportToFile());

        JButton btnBlindar = new JButton("BLINDAR (KEK)");
        btnBlindar.putClientProperty(FlatClientProperties.STYLE, "background: #107C10; foreground: #FFFFFF; hoverBackground: #0B590B; arc: 16");
        btnBlindar.addActionListener(e -> exportBlindada());
        
        buttonRow.add(btnCopy);
        buttonRow.add(btnExport);
        buttonRow.add(btnBlindar);
        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = row++;
        mainPanel.add(buttonRow, gbc);

        add(mainPanel, BorderLayout.CENTER);
        pack();
    }

    private void generateCek() {
        currentCek = RSALogica.generarCEK();
        String b64 = Base64.getEncoder().encodeToString(currentCek);
        txtCekDisplay.setText(b64);
        ToastNotification.show(this, "CEK Generada con Éxito", ToastNotification.Type.SUCCESS);
    }

    private void copyToClipboard() {
        if (currentCek == null) return;
        String b64 = Base64.getEncoder().encodeToString(currentCek);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(b64), null);
        ToastNotification.show(this, "Llave copiada al portapapeles (expira en 30s)", ToastNotification.Type.SUCCESS);
        javax.swing.Timer purgeTimer = new javax.swing.Timer(30000, evt -> {
            try {
                java.awt.datatransfer.Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (clip.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                    String cur = (String) clip.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (b64.equals(cur)) clip.setContents(new StringSelection(""), null);
                }
            } catch (Exception ignored) {}
        });
        purgeTimer.setRepeats(false);
        purgeTimer.start();
    }

    private void exportToFile() {
        if (currentCek == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar Llave CEK (Plano)");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File target = fc.getSelectedFile();
                if (!target.getName().endsWith(".key")) {
                    target = new File(target.getAbsolutePath() + ".key");
                }
                Files.write(target.toPath(), currentCek);
                ToastNotification.show(this, "Llave exportada: " + target.getName(), ToastNotification.Type.SUCCESS);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportBlindada() {
        if (currentCek == null) return;
        
        JPasswordField passField = new JPasswordField();
        Object[] message = {"Ingrese frase de paso para blindar la CEK (KEK):", passField};
        int option = JOptionPane.showConfirmDialog(this, message, "Blindaje con KEK", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            char[] pass = passField.getPassword();
            if (pass.length < 8) {
                JOptionPane.showMessageDialog(this, "La frase de paso debe tener al menos 8 caracteres.", "Seguridad Insuficiente", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                byte[] salt = SecuritySession.getUserSalt();
                byte[] wrapped;
                if (salt != null) {
                    wrapped = RSALogica.wrapCEK(currentCek, pass, salt);
                } else {
                    wrapped = RSALogica.wrapCEK(currentCek, pass, RSALogica.getUniversalSalt().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Guardar Llave Blindada (.wcek)");
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File target = fc.getSelectedFile();
                    if (!target.getName().endsWith(".wcek")) {
                        target = new File(target.getAbsolutePath() + ".wcek");
                    }
                    Files.write(target.toPath(), wrapped);
                    ToastNotification.show(this, "Llave BLINDADA con éxito", ToastNotification.Type.SUCCESS);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Fallo en el blindaje: " + e.getMessage());
            } finally {
                java.util.Arrays.fill(pass, '0');
            }
        }
    }
}

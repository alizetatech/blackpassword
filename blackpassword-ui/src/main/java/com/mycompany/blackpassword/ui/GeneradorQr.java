package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mycompany.blackpassword.core.util.ToastNotification;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class GeneradorQr extends javax.swing.JInternalFrame {

    private JPasswordField txtInput;
    private JLabel lblQrImage;
    private JButton btnGenerate;
    private JButton btnExport;
    private BufferedImage currentQrImage;

    public GeneradorQr() {
        super("Generador de Códigos QR", true, true, true, true);
        initComponentsCustom();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);

        txtInput.putClientProperty(FlatClientProperties.STYLE, ""
                + "font: +1;"
                + "background: #F8F9FA;"
                + "border: 10,10,10,10;"
                + "showRevealButton: true");

        btnGenerate.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");

        btnExport.putClientProperty(FlatClientProperties.STYLE, 
                "arc: 16; background: #F3F3F3; foreground: #333333; hoverBackground: #E5E5E5; borderWidth: 0; font: bold 12");

        try {
            btnGenerate.setIcon(new FlatSVGIcon("img/security.svg", 18, 18)); // Reusing SVG available
            btnExport.setIcon(new FlatSVGIcon("img/report.svg", 18, 18));
        } catch (Exception e) {}
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        JLabel lblTitle = new JLabel("GENERADOR DE QR SEGURO");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 22; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 5, 0); gbc.gridy = row++;
        mainPanel.add(lblTitle, gbc);

        JLabel lblInfo = new JLabel("Cree códigos QR de alta resolución a partir de cualquier texto, URL o secreto.");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setForeground(new Color(100, 100, 100));
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        mainPanel.add(lblInfo, gbc);

        txtInput = new JPasswordField(30);
        txtInput.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.insets = new Insets(0, 0, 15, 0); gbc.gridy = row++;
        gbc.ipady = 15; // Make the password field a bit taller visually
        mainPanel.add(txtInput, gbc);
        gbc.ipady = 0;

        btnGenerate = new JButton("GENERAR CÓDIGO QR");
        btnGenerate.setPreferredSize(new Dimension(250, 45));
        btnGenerate.addActionListener(e -> generateQr());
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        mainPanel.add(btnGenerate, gbc);

        lblQrImage = new JLabel();
        lblQrImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblQrImage.setPreferredSize(new Dimension(250, 250));
        lblQrImage.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        mainPanel.add(lblQrImage, gbc);

        btnExport = new JButton("EXPORTAR COMO PNG");
        btnExport.setPreferredSize(new Dimension(250, 40));
        btnExport.addActionListener(e -> exportToFile());
        btnExport.setEnabled(false);
        gbc.insets = new Insets(0, 0, 0, 0); gbc.gridy = row++;
        mainPanel.add(btnExport, gbc);

        add(mainPanel, BorderLayout.CENTER);
        pack();
    }

    private void generateQr() {
        String data = new String(txtInput.getPassword());
        if (data == null || data.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, introduzca el texto a codificar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction
            hints.put(EncodeHintType.MARGIN, 1);

            int width = 300;
            int height = 300;
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
            currentQrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);
            
            Image scaledImage = currentQrImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            lblQrImage.setIcon(new ImageIcon(scaledImage));
            btnExport.setEnabled(true);
            
            ToastNotification.show(this, "QR Generado Exitosamente", ToastNotification.Type.SUCCESS);

        } catch (WriterException e) {
            JOptionPane.showMessageDialog(this, "Error al generar el QR: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToFile() {
        if (currentQrImage == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar QR como Imagen");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File target = fc.getSelectedFile();
                if (!target.getName().toLowerCase().endsWith(".png")) {
                    target = new File(target.getAbsolutePath() + ".png");
                }
                ImageIO.write(currentQrImage, "PNG", target);
                ToastNotification.show(this, "Imagen guardada: " + target.getName(), ToastNotification.Type.SUCCESS);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

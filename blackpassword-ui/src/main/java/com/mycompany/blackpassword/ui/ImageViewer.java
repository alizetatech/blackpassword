package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.JDesktopPane; // Importación añadida

/**
 * Visor de Imágenes Seguro v9.9-LUXURY.
 * Muestra imágenes descifradas y permite establecerlas como fondo del dashboard.
 */
public class ImageViewer extends JInternalFrame {

    private final File imageFile;
    private final JLabel lblImage;
    private final MenuPrincipal parentDashboard;

    public ImageViewer(MenuPrincipal dashboard, File file) {
        super("Visor de Imágenes: " + file.getName(), true, true, true, true);
        this.parentDashboard = dashboard;
        this.imageFile = file;
        this.lblImage = new JLabel("", SwingConstants.CENTER);
        
        initComponents();
        applyPremiumStyle();
        // loadImage(); // Se llamará externamente después de pack()
    }

    private void initComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(lblImage), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton btnSetBackground = new JButton("🖼️ Establecer como Fondo");
        btnSetBackground.addActionListener(e -> setAsBackground());
        buttonPanel.add(btnSetBackground);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(contentPanel);
        
        setSize(800, 600);
        setMinimumSize(new Dimension(400, 300));
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                loadImage(); // Recargar imagen al redimensionar para ajustar tamaño
            }
        });
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        if (this.getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI ui) {
            ui.setNorthPane(null);
        }
    }

    public void loadImage() { // Método hecho público
        if (imageFile == null || !imageFile.exists()) {
            lblImage.setText("No se pudo cargar la imagen.");
            lblImage.setIcon(null);
            return;
        }
        try {
            ImageIcon originalIcon = new ImageIcon(imageFile.getAbsolutePath());
            Image originalImage = originalIcon.getImage();

            int panelWidth = lblImage.getWidth() > 0 ? lblImage.getWidth() : getWidth();
            int panelHeight = lblImage.getHeight() > 0 ? lblImage.getHeight() : getHeight() - 50; // Restar altura de botones

            if (panelWidth <= 0 || panelHeight <= 0) return; // Evitar errores de tamaño 0

            Image scaledImage = originalImage.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
            lblImage.setIcon(new ImageIcon(scaledImage));
            lblImage.setText("");
        } catch (Exception e) {
            lblImage.setText("Error al mostrar la imagen: " + e.getMessage());
            lblImage.setIcon(null);
        }
    }

    private void setAsBackground() {
        if (parentDashboard != null && imageFile != null) {
            parentDashboard.setDashboardBackground(imageFile);
            JOptionPane.showMessageDialog(this, "Imagen establecida como fondo del dashboard.", "Fondo Personalizado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo establecer la imagen como fondo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void centerInParent() {
        if (getParent() instanceof JDesktopPane desktop) {
            int x = (desktop.getWidth() - getWidth()) / 2;
            int y = (desktop.getHeight() - getHeight()) / 2;
            setLocation(Math.max(0, x), Math.max(0, y));
        }
    }
}

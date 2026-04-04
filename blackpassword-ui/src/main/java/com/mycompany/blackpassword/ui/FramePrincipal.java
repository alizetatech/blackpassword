package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ventana principal v6.3-PLATINUM.
 * Estética Premium reforzada con Capa de Inyección 2.
 * Incluye acceso directo al Cifrador de Archivos AES-256.
 */
public class FramePrincipal extends javax.swing.JFrame {
    
    private Image backgroundImage = null;
    private boolean camouflageActive = false;

    public FramePrincipal() {
        // CAPA DE INYECCIÓN 2: Refuerzo de tema
        if (!(UIManager.getLookAndFeel() instanceof FlatLaf)) {
            FlatLightLaf.setup();
        }
        
        cargarImagenFondo();
        initComponents();
        applyPremiumStyle();
        setupApp();
        setupMenuActions();
    }
    
    private void cargarImagenFondo() {
        try {
            java.net.URL imgUrl = getClass().getResource("/img/blackpassword.png");
            if (imgUrl != null) backgroundImage = javax.imageio.ImageIO.read(imgUrl);
        } catch (Exception e) {}
    }

    public void setCamouflageMode(boolean active) {
        this.camouflageActive = active;
        FlatAnimatedLafChange.showSnapshot();
        this.setTitle(active ? "Calculadora" : "Blackpassword - Suite de Seguridad Premium");
        jDesktopPane1.repaint();
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }
    
    private void applyPremiumStyle() {
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, UIManager.getColor("Panel.background"));
        jMenuBar1.putClientProperty(FlatClientProperties.STYLE, "margin: 3,3,3,3; border: 0,0,1,0, #E0E0E0");
        jDesktopPane1.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
    }

    private void setupApp() {
        this.setTitle("Blackpassword - Suite de Seguridad Premium v6.3");
        this.setExtendedState(MAXIMIZED_BOTH);
        
        jDesktopPane1.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                for (JInternalFrame f : jDesktopPane1.getAllFrames()) if (f.isVisible() && !f.isMaximum()) centerInternalFrame(f);
            }
        });

        Inicio inicio = new Inicio();
        jDesktopPane1.add(inicio);
        inicio.setVisible(true);
        java.awt.EventQueue.invokeLater(() -> centerInternalFrame(inicio));
    }
    
    public void centerInternalFrame(JInternalFrame frame) {
        int x = (jDesktopPane1.getWidth() - frame.getWidth()) / 2;
        int y = (jDesktopPane1.getHeight() - frame.getHeight()) / 2;
        frame.setLocation(Math.max(0, x), Math.max(0, y));
    }

    private void setupMenuActions() {
        jMenu2.removeAll();
        
        JMenuItem itemGenRsa = new JMenuItem("🔑 Generador de Llaves RSA");
        itemGenRsa.addActionListener(e -> {
            GeneradorLlavesRsa rsaFrame = new GeneradorLlavesRsa();
            jDesktopPane1.add(rsaFrame); centerInternalFrame(rsaFrame); rsaFrame.setVisible(true);
        });
        jMenu2.add(itemGenRsa);

        JMenuItem itemCifrador = new JMenuItem("📁 Cifrador de Archivos AES-256");
        itemCifrador.addActionListener(e -> {
            CifradorArchivo cifrador = new CifradorArchivo("Invitado");
            jDesktopPane1.add(cifrador); centerInternalFrame(cifrador); cifrador.setVisible(true);
        });
        jMenu2.add(itemCifrador);

        JMenuItem itemEncriptador = new JMenuItem("🔐 Encriptador de Algoritmos");
        itemEncriptador.addActionListener(e -> {
            EncriptadorDeAlgoritmos encriptador = new EncriptadorDeAlgoritmos();
            jDesktopPane1.add(encriptador); centerInternalFrame(encriptador); encriptador.setVisible(true);
        });
        jMenu2.add(itemEncriptador);

        jMenu3.removeAll();
        JMenuItem itemLight = new JMenuItem("☀️ Modo Claro");
        itemLight.addActionListener(e -> changeTheme(new FlatLightLaf()));
        JMenuItem itemDark = new JMenuItem("🌙 Modo Oscuro");
        itemDark.addActionListener(e -> changeTheme(new FlatDarkLaf()));
        jMenu3.add(itemLight); jMenu3.add(itemDark);
    }

    private void changeTheme(FlatLaf laf) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            UIManager.setLookAndFeel(laf);
            FlatLaf.updateUI();
            SwingUtilities.updateComponentTreeUI(this);
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Exception ex) {}
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jDesktopPane1 = new javax.swing.JDesktopPane() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (camouflageActive) {
                    g.setColor(new Color(240, 240, 240)); g.fillRect(0, 0, getWidth(), getHeight());
                } else if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    int iw = backgroundImage.getWidth(this), ih = backgroundImage.getHeight(this);
                    double scale = Math.max((double) getWidth() / iw, (double) getHeight() / ih);
                    int sw = (int) (iw * scale), sh = (int) (ih * scale);
                    g2d.drawImage(backgroundImage, (getWidth() - sw) / 2, (getHeight() - sh) / 2, sw, sh, this);
                }
            }
        };
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu(); jMenu2 = new javax.swing.JMenu(); jMenu3 = new javax.swing.JMenu();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        jMenu1.setText("Archivo"); jMenuBar1.add(jMenu1);
        jMenu2.setText("Seguridad"); jMenuBar1.add(jMenu2);
        jMenu3.setText("Apariencia"); jMenuBar1.add(jMenu3);
        setJMenuBar(jMenuBar1);
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(jDesktopPane1, java.awt.BorderLayout.CENTER);
        pack();
    }

    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
}

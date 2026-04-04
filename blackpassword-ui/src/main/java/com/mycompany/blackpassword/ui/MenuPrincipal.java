package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.core.AnimationUtils;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.LocalDatabaseService;
import com.mycompany.blackpassword.services.RemoteKillService;
import com.mycompany.blackpassword.services.SecuritySession;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;

/**
 * Dashboard Blindado v12.1: Reversión de Suscripciones.
 * Acceso total e ilimitado a todos los módulos de seguridad.
 */
public class MenuPrincipal extends javax.swing.JInternalFrame {

    static {
        System.out.println("[v12.1] Dashboard cargado. Acceso total habilitado.");
    }

    private final String userEmail;
    private final String userName;
    private Image backgroundImage = null;
    private final Preferences prefs = Preferences.userNodeForPackage(MenuPrincipal.class);
    
    private Timer inactivityTimer;
    private AWTEventListener globalActivityListener;
    private final int INACTIVITY_TIMEOUT = 5 * 60 * 1000;
    
    private JPanel privacyShield;
    private boolean camouflageActive = false;
    private JLabel lblSecurityStatus;

    public MenuPrincipal(String nombre, String email) {
        this.userName = nombre;
        this.userEmail = email;
        
        try {
            validarIntegridadIdentidad(); 
            LocalDatabaseService.init();
            initComponents();
            applyPremiumStyle();
            setupInternalCentering();
            setupTopNavigation();
            setupPrivacyShield();
            cargarFondoPersistente();
            setupInactivityTimer();
            setupPanicShortcut();
            setupFocusSecurity();
            setupCamouflageShortcut();
            setupRemoteKillMonitoring();
        } catch (SecurityException se) {
            ejecutarPanico(); 
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void validarIntegridadIdentidad() {
        if (!SecuritySession.isIdentityValid(userEmail)) {
            AuditService.log(userEmail, "IDENTITY_VIOLATION", "Fallo en la validación de identidad cruzada.");
            JOptionPane.showMessageDialog(null, "🚨 ERROR CRÍTICO DE IDENTIDAD:\nLas huellas digitales del token no coinciden.\nPor seguridad, la aplicación se cerrará.", "Violación de Seguridad", JOptionPane.ERROR_MESSAGE);
            throw new SecurityException("IDENTITY_MISMATCH");
        }
        
        if (lblSecurityStatus != null) {
            lblSecurityStatus.setText("IDENTIDAD BLINDADA ✅");
            lblSecurityStatus.setForeground(new Color(16, 124, 16));
        }
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        if (this.getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI ui) {
            ui.setNorthPane(null);
        }
    }

    private void setupRemoteKillMonitoring() {
        try {
            RemoteKillService.startMonitoring(userEmail, () -> {
                LocalDatabaseService.clearCache(userEmail);
                limpiarRecursosSeguridad();
            });
        } catch (Exception e) {}
    }

    private void setupPrivacyShield() {
        privacyShield = new JPanel(new BorderLayout());
        privacyShield.setBackground(new Color(10, 10, 10, 230));
        JLabel lblShield = new JLabel("MODO PRIVACIDAD ACTIVO", SwingConstants.CENTER);
        lblShield.setForeground(Color.WHITE);
        lblShield.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        privacyShield.add(lblShield, BorderLayout.CENTER);
        privacyShield.setVisible(false);
        this.getLayeredPane().add(privacyShield, javax.swing.JLayeredPane.DRAG_LAYER);
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { privacyShield.setBounds(0, 0, getWidth(), getHeight()); }
        });
    }

    private void setupFocusSecurity() {
        java.awt.EventQueue.invokeLater(() -> {
            java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (parent != null) {
                parent.addWindowFocusListener(new WindowFocusListener() {
                    @Override public void windowGainedFocus(WindowEvent e) { if (!camouflageActive) privacyShield.setVisible(false); }
                    @Override public void windowLostFocus(WindowEvent e) {
                        if (SecuritySession.isSessionActive() && !camouflageActive) privacyShield.setVisible(true);
                    }
                });
            }
        });
    }

    private void setupPanicShortcut() {
        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "panic");
        this.getActionMap().put("panic", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { ejecutarPanico(); }
        });
    }

    private void setupCamouflageShortcut() {
        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "camouflage");
        this.getActionMap().put("camouflage", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCamouflage(); }
        });
    }

    private void toggleCamouflage() {
        camouflageActive = !camouflageActive;
        java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (parent instanceof FramePrincipal fp) fp.setCamouflageMode(camouflageActive);
        for (JInternalFrame f : internalDesktop.getAllFrames()) f.setVisible(!camouflageActive);
    }

    private void setupInactivityTimer() {
        inactivityTimer = new Timer(INACTIVITY_TIMEOUT, e -> ejecutarCierreSesion());
        inactivityTimer.setRepeats(false);
        globalActivityListener = event -> { if (inactivityTimer != null && inactivityTimer.isRunning()) inactivityTimer.restart(); };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalActivityListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        inactivityTimer.start();
    }

    private void ejecutarCierreSesion() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Desea cerrar su sesión segura?", "Cerrar Sesión", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            limpiarRecursosSeguridad();
            javax.swing.JDesktopPane mainDesktop = this.getDesktopPane();
            this.dispose();
            if (mainDesktop != null) {
                Inicio inicio = new Inicio(); 
                mainDesktop.add(inicio); 
                inicio.setVisible(true);
                try { inicio.setSelected(true); } catch (Exception e) {}
            }
        }
    }

    private void ejecutarPanico() {
        limpiarRecursosSeguridad();
        System.exit(0);
    }

    private void limpiarRecursosSeguridad() {
        if (inactivityTimer != null) inactivityTimer.stop();
        if (globalActivityListener != null) Toolkit.getDefaultToolkit().removeAWTEventListener(globalActivityListener);
        LocalDatabaseService.closeAndEncrypt();
        SecuritySession.clear();
        AuditService.log(userEmail, "LOGOUT_SUCCESS", "Sesión cerrada, BD cifrada y llaves purgadas de RAM.");
    }

    private void cargarFondoPersistente() {
        try {
            String path = prefs.get("bg_path_" + userEmail, "");
            if (!path.isEmpty() && new File(path).exists()) {
                backgroundImage = ImageIO.read(new File(path));
            } else {
                java.net.URL imgUrl = getClass().getResource("/img/blackpassword.png");
                if (imgUrl != null) backgroundImage = ImageIO.read(imgUrl);
            }
        } catch (Exception e) {
            System.err.println("[MenuPrincipal] Error al cargar fondo persistente: " + e.getMessage());
        }
    }
    
    public void setDashboardBackground(File imageFile) {
        try {
            this.backgroundImage = ImageIO.read(imageFile);
            prefs.put("bg_path_" + userEmail, imageFile.getAbsolutePath());
            internalDesktop.repaint();
            AuditService.log(userEmail, "DASHBOARD_BACKGROUND_SET", "Fondo de dashboard personalizado.");
        } catch (Exception e) {
            System.err.println("[MenuPrincipal] Error al establecer fondo: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al establecer la imagen como fondo: " + e.getMessage(), "Error de Fondo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupInternalCentering() {
        internalDesktop.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                for (JInternalFrame f : internalDesktop.getAllFrames()) {
                    if (f.isVisible() && !f.isMaximum()) {
                        packAndCenter(f);
                    }
                }
            }
        });
    }

    private void setupTopNavigation() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(getWidth(), 75));
        topBar.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,1,0,#EBEBEB; margin: 0,15,0,15");
        
        JPanel buttonsContainer = new JPanel(new BorderLayout());
        buttonsContainer.setOpaque(false);
        
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 18));
        leftGroup.setOpaque(false);
        
        leftGroup.add(createNavButton("BÓVEDA", "vault.svg", e -> solicitarPinParaBoveda()));
        leftGroup.add(createNavButton("ARCHIVOS", "files.svg", e -> abrirVentanaUnica(Archivos.class, () -> new Archivos(userEmail))));
        leftGroup.add(createNavButton("IMÁGENES", "images.svg", e -> abrirVentanaUnica(Imagenes.class, () -> new Imagenes(userEmail, this))));
        
        // Botón de Seguridad con Menú Desplegable
        JButton btnSecurity = createNavButton("SEGURIDAD", "security.svg", null);
        JPopupMenu securityMenu = new JPopupMenu();
        securityMenu.add(createMenuItem("GENERADOR PGP", "key.svg", e -> abrirVentanaUnica(GeneradorPgp.class, () -> new GeneradorPgp())));
        securityMenu.add(createMenuItem("GENERADOR RSA", "rsa.svg", e -> abrirVentanaUnica(GeneradorLlavesRsa.class, () -> new GeneradorLlavesRsa())));
        securityMenu.add(createMenuItem("ENCRIPTADOR ALGORITMOS", "shield.svg", e -> abrirVentanaUnica(EncriptadorDeAlgoritmos.class, () -> new EncriptadorDeAlgoritmos())));
        
        btnSecurity.addActionListener(e -> securityMenu.show(btnSecurity, 0, btnSecurity.getHeight()));
        leftGroup.add(btnSecurity);
        
        leftGroup.add(createNavButton("REPORTES", "report.svg", e -> abrirVentanaUnica(Reportes.class, () -> new Reportes(userEmail))));
        
        JPanel centerGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 22));
        centerGroup.setOpaque(false);
        
        lblSecurityStatus = new JLabel("ID VERIFICADA");
        lblSecurityStatus.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #107C10; background: #DFF6DD; border: 4,12,4,12, #DFF6DD, 1, 20");
        centerGroup.add(lblSecurityStatus);
        
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 18));
        rightGroup.setOpaque(false);
        
        rightGroup.add(createNavButton("SALIR", "unlock.svg", e -> ejecutarCierreSesion()));
        
        JButton btnUser = createNavButton(userName.toUpperCase(), "user.svg", null);
        btnUser.putClientProperty(FlatClientProperties.STYLE, "foreground: #0078D4; font: bold; background: #F3F9FF; arc: 20");
        rightGroup.add(btnUser);
        
        buttonsContainer.add(leftGroup, BorderLayout.WEST);
        buttonsContainer.add(centerGroup, BorderLayout.CENTER);
        buttonsContainer.add(rightGroup, BorderLayout.EAST);
        topBar.add(buttonsContainer, BorderLayout.CENTER);
        getContentPane().add(topBar, BorderLayout.NORTH);
    }

    private JButton createNavButton(String text, String iconName, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        try {
            FlatSVGIcon icon = new FlatSVGIcon("img/" + iconName, 20, 20);
            if (icon != null) btn.setIcon(icon);
        } catch (Exception e) {}
        if (action != null) btn.addActionListener(action);
        btn.putClientProperty(FlatClientProperties.STYLE, 
            "buttonType: toolBarButton; " +
            "arc: 16; " +
            "margin: 8,16,8,16; " +
            "font: bold 12; " +
            "hoverForeground: #0078D4; " +
            "hoverBackground: #F5F5F5; " +
            "focusedBackground: #F0F0F0");
        return btn;
    }

    private JMenuItem createMenuItem(String text, String iconName, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        try {
            FlatSVGIcon icon = new FlatSVGIcon("img/" + iconName, 18, 18);
            item.setIcon(icon);
        } catch (Exception e) {}
        if (action != null) item.addActionListener(action);
        return item;
    }

    private void solicitarPinParaBoveda() {
        validarIntegridadIdentidad(); 

        JPasswordField pinField = new JPasswordField(4);
        pinField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true;");
        Object[] message = {"Para acceder a la bóveda, ingrese su PIN de seguridad:", pinField};
        
        int option = JOptionPane.showConfirmDialog(this, message, "Verificación de Seguridad", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            char[] pin = pinField.getPassword();
            if (SecuritySession.verifyUserPin(pin)) {
                AuditService.log(userEmail, "VAULT_ACCESS_SUCCESS", "Acceso a la bóveda autorizado por PIN.");
                abrirVentanaUnica(Contrasenas.class, () -> new Contrasenas(userEmail));
            } else {
                AuditService.log(userEmail, "VAULT_ACCESS_DENIED", "Intento de acceso a la bóveda con PIN incorrecto.");
                JOptionPane.showMessageDialog(this, "PIN incorrecto. Acceso denegado.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
            }
            Arrays.fill(pin, '0'); 
        }
    }

    private void abrirVentanaUnica(Class<?> claseVentana, java.util.function.Supplier<JInternalFrame> creador) {
        for (JInternalFrame f : internalDesktop.getAllFrames()) {
            if (claseVentana.isInstance(f)) {
                try {
                    f.setSelected(true); f.toFront(); return;
                } catch (Exception e) {}
            }
        }
        try {
            JInternalFrame nueva = creador.get();
            internalDesktop.add(nueva);
            
            if (nueva instanceof Contrasenas || nueva instanceof Archivos || nueva instanceof Imagenes) {
                try {
                    nueva.setMaximum(true);
                } catch (Exception e) {
                    packAndCenter(nueva);
                }
            } else {
                packAndCenter(nueva);
            }
            
            nueva.setVisible(true);
            AnimationUtils.fadeInAndSlide(nueva, internalDesktop);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al abrir la ventana: " + e.getMessage(), "Error de UI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initComponents() {
        internalDesktop = new javax.swing.JDesktopPane() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null && !camouflageActive) {
                    Graphics2D g2d = (Graphics2D) g;
                    int iw = backgroundImage.getWidth(this), ih = backgroundImage.getHeight(this);
                    double scale = Math.max((double)getWidth()/iw, (double)getHeight()/ih);
                    g2d.drawImage(backgroundImage, (getWidth()-(int)(iw*scale))/2, (getHeight()-(int)(ih*scale))/2, (int)(iw*scale), (int)(ih*scale), this);
                }
            }
        };
        setClosable(false); setIconifiable(true); setMaximizable(true); setResizable(true);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(internalDesktop, BorderLayout.CENTER);
        
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void packAndCenter(JInternalFrame frame) {
        frame.pack();
        
        // Si el frame es más grande que el desktop, lo limitamos al 90%
        int dw = internalDesktop.getWidth();
        int dh = internalDesktop.getHeight();
        int fw = frame.getWidth();
        int fh = frame.getHeight();
        
        if (fw > dw * 0.95) fw = (int)(dw * 0.9);
        if (fh > dh * 0.95) fh = (int)(dh * 0.9);
        
        frame.setSize(fw, fh);
        frame.setLocation((dw - fw) / 2, (dh - fh) / 2);
    }

    private javax.swing.JDesktopPane internalDesktop;
}

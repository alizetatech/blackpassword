package com.mycompany.blackpassword.core;

import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.ui.FramePrincipal;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de Blackpassword v9.0-FINAL-FIX.
 * Implementa Blindaje de Red de Nivel 9 (Inmunidad Total Windows 11).
 */
public class Blackpassword {

    static {
        // CAPA 0: BLINDAJE DE RED ABSOLUTO (ANTI-UNIX-SOCKET-BUG)
        try {
            String dns = "dns";
            // Configuración de gRPC para Windows (Inmunidad Total)
            System.setProperty("io.grpc.NameResolverRegistry.defaultScheme", dns);
            System.setProperty("io.grpc.netty.shaded.io.grpc.NameResolverRegistry.defaultScheme", dns);
            System.setProperty("io.grpc.netty.UdsNameResolverProvider.enabled", "false");
            System.setProperty("io.grpc.netty.shaded.io.grpc.netty.UdsNameResolverProvider.enabled", "false");
            System.setProperty("io.grpc.internal.DnsNameResolverProvider.enable_jndi", "false");
            System.setProperty("io.grpc.internal.DnsNameResolverProvider.enable_proxy_detector", "false");
            System.setProperty("io.grpc.ManagedChannelRegistry.useOkHttp", "true");
            System.setProperty("io.netty.transport.noNative", "true");
            System.setProperty("io.netty.shaded.io.netty.transport.noNative", "true");
            System.setProperty("io.netty.noUnsafe", "true");
            System.setProperty("io.grpc.internal.ManagedChannelImplBuilder.enable_unix_domain_socket", "false");

            // --- REFUERZO VISUAL PREMIUM (TOKEN DE DISEÑO) ---
            UIManager.put("Button.arc", 16);
            UIManager.put("Component.arc", 16);
            UIManager.put("TextComponent.arc", 16);
            UIManager.put("CheckBox.arc", 8);
            UIManager.put("ProgressBar.arc", 12);
            
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("Component.innerFocusWidth", 1);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            
            // Colores Premium (Azure/Deep Dark)
            UIManager.put("AccentColor", "#0078D4");
            UIManager.put("DefaultFont", new java.awt.Font("Segoe UI Variable Display", java.awt.Font.PLAIN, 13));

            System.out.println("[v9.1] Capa 0: Blindaje de Red + Diseño Nivel 10 Activo.");
        } catch (Exception e) {
            System.err.println("Error en Capa 0: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 0. CAPA DE SEGURIDAD HSM: REGISTRO DE AUTO-DESTRUCCIÓN
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[HSM] 🛡️ Cierre detectado. Cifrando base de datos local e incinerando llaves físicas...");
            try {
                com.mycompany.blackpassword.services.LocalDatabaseService.closeAndEncrypt();
            } catch (Exception ignored) {}
            com.mycompany.blackpassword.services.hsm.KeyAnchorService.getInstance().destroyAll();
            com.mycompany.blackpassword.services.SecuritySession.clear();
        }));

        // REGISTRO DE NOTIFICACIONES (Audit -> UI)
        AuditService.setNotificationListener((accion, detalles) -> {
            SwingUtilities.invokeLater(() -> {
                Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                if (win == null) {
                    for (Window w : Window.getWindows()) {
                        if (w.isVisible()) { win = w; break; }
                    }
                }
                if (win == null) return;

                String msgCorto = (detalles.length() > 45) ? detalles.substring(0, 42) + "..." : detalles;
                if (accion.contains("ERROR") || accion.contains("VIOLATION") || accion.contains("KILL")) {
                    ToastNotification.show(win, "ALERTA: " + msgCorto, ToastNotification.Type.ERROR);
                } else if (accion.contains("SUCCESS") || accion.contains("UPDATE") || accion.contains("LOGIN")) {
                    ToastNotification.show(win, msgCorto, ToastNotification.Type.SUCCESS);
                } else if (accion.contains("COPY")) {
                    ToastNotification.show(win, "Copiado al portapapeles", ToastNotification.Type.INFO);
                } else {
                    ToastNotification.show(win, msgCorto, ToastNotification.Type.INFO);
                }
            });
        });

        // 1. CAPA VISUAL
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            // Forzar modo claro de lujo por defecto
            FlatLightLaf.setup();
            
            // Personalización fina de FlatLaf
            UIManager.put("Button.hoverBackground", "#F0F0F0");
            UIManager.put("Button.pressedBackground", "#E0E0E0");
            UIManager.put("Button.focusPainted", Boolean.FALSE);
            UIManager.put("ToggleButton.focusPainted", Boolean.FALSE);
            UIManager.put("Button.showFocusOwner", Boolean.FALSE);
            UIManager.put("Button.focusWidth", 0);
            UIManager.put("Button.innerFocusWidth", 0);
            UIManager.put("Button.focusedBorderColor", null);
            UIManager.put("Button.default.focusedBorderColor", null);
            UIManager.put("Button.focusedBackground", null);
            UIManager.put("Button.default.focusedBackground", null);
            UIManager.put("TableHeader.background", "#FFFFFF");
            UIManager.put("Table.selectionBackground", "#E1F0FF");
            UIManager.put("Table.selectionForeground", "#005A9E");
            
            // Comportamiento anti-selección de botones (no se quedan con borde/anillo de foco al hacer clic)
            setupAntiBotonSeleccionado();
            
            System.out.println("[v9.1] Capa 1: Motor Visual Refinado + Anti-Selección de Botones.");
        } catch (Exception e) {
            System.err.println("Fallo en Capa 1: " + e.getMessage());
        }

        // 2. GESTIÓN DE ERRORES CRÍTICOS
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            final String msg = e.toString();
            SwingUtilities.invokeLater(() -> {
                if (msg.contains("nameresolver unix")) {
                    JOptionPane.showMessageDialog(null, 
                        "🚨 ERROR DE RED gRPC PERSISTENTE\n" +
                        "El sistema sigue intentando usar protocolos de Linux.\n" +
                        "SOLUCIÓN: Haga 'Clean and Build' OBLIGATORIO en NetBeans.", 
                        "Error de Compatibilidad", JOptionPane.ERROR_MESSAGE);
                } else if (msg.contains("NoClassDefFound") || msg.contains("NoSuchMethod")) {
                    JOptionPane.showMessageDialog(null, 
                        "🚨 ERROR DE EMPAQUETADO:\n" +
                        "Faltan dependencias en el archivo JAR.\n" +
                        "Se recomienda recompilar el proyecto.", 
                        "Fallo de Sistema", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "🚨 DETALLES DEL ERROR:\n" + msg, "Seguridad Blackpassword", JOptionPane.WARNING_MESSAGE);
                }
            });
            AuditService.log("SYSTEM", "UNCAUGHT_EXCEPTION", msg);
        });

        System.out.println("\n--- INICIANDO BLACKPASSWORD v9.0-FINAL-FIX ---");
        
        java.awt.EventQueue.invokeLater(() -> {
            try {
                FramePrincipal frame = new FramePrincipal();
                SwingUtilities.updateComponentTreeUI(frame);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void setupAntiBotonSeleccionado() {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event instanceof MouseEvent me) {
                    if (me.getSource() instanceof AbstractButton btn) {
                        btn.setFocusPainted(false);
                        btn.setRequestFocusEnabled(false);
                        if (me.getID() == MouseEvent.MOUSE_RELEASED || me.getID() == MouseEvent.MOUSE_CLICKED) {
                            if (btn.isFocusOwner()) {
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                            }
                        }
                    }
                }
            }, AWTEvent.MOUSE_EVENT_MASK);

            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event instanceof HierarchyEvent he) {
                    if ((he.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && he.getComponent().isShowing()) {
                        desactivarFocoBotones(he.getComponent());
                    }
                }
            }, AWTEvent.HIERARCHY_EVENT_MASK);
        } catch (Exception e) {
            System.err.println("Error configurando comportamiento de foco en botones: " + e.getMessage());
        }
    }

    private static void desactivarFocoBotones(Component comp) {
        if (comp instanceof AbstractButton btn) {
            btn.setFocusPainted(false);
            btn.setRequestFocusEnabled(false);
        }
        if (comp instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                desactivarFocoBotones(child);
            }
        }
    }
}

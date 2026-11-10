package com.mycompany.blackpassword.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * Gestor de Perfiles de Usuario v6.5 (Zero-Disk Edition).
 * Opera exclusivamente en memoria RAM durante la sesión activa y purga
 * cualquier rastro residual en el Registro del sistema operativo.
 */
public class ProfileManager {
    private static final Map<String, Profile> sessionProfiles = new ConcurrentHashMap<>();

    static {
        try {
            Preferences legacyPrefs = Preferences.userNodeForPackage(ProfileManager.class);
            legacyPrefs.clear();
            legacyPrefs.flush();
        } catch (Exception ignored) {}
    }

    public static class Profile {
        public String name;
        public String firebasePath;
        public String pubKeyPath;
        public String privKeyPath;

        public Profile(String name, String firebasePath, String pubKeyPath, String privKeyPath) {
            this.name = name;
            this.firebasePath = firebasePath;
            this.pubKeyPath = pubKeyPath;
            this.privKeyPath = privKeyPath;
        }
        
        @Override
        public String toString() { return name; }
    }

    public static void saveProfile(Profile p) {
        if (p != null && p.name != null && !p.name.isBlank()) {
            sessionProfiles.put(p.name, p);
        }
    }

    public static List<Profile> getProfiles() {
        return new ArrayList<>(sessionProfiles.values());
    }
    
    public static void deleteProfile(String name) {
        if (name != null) {
            sessionProfiles.remove(name);
        }
    }
}

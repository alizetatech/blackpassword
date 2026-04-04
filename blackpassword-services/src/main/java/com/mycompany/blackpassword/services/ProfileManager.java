package com.mycompany.blackpassword.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Gestor de Perfiles de Usuario v5.4.
 * Maneja de forma robusta los atajos locales a las llaves y configuración.
 */
public class ProfileManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(ProfileManager.class);
    private static final String PROFILES_KEY = "saved_profiles_v54";

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
        String base = "p54_" + p.name + "_";
        prefs.put(base + "fb", p.firebasePath);
        prefs.put(base + "pub", p.pubKeyPath);
        prefs.put(base + "priv", p.privKeyPath);
        
        List<String> names = getProfileNames();
        if (!names.contains(p.name)) {
            names.add(p.name);
            saveProfileNames(names);
        }
    }

    public static List<Profile> getProfiles() {
        return getProfileNames().stream()
            .map(name -> {
                String base = "p54_" + name + "_";
                return new Profile(name, 
                    prefs.get(base + "fb", ""),
                    prefs.get(base + "pub", ""),
                    prefs.get(base + "priv", "")
                );
            }).collect(Collectors.toList());
    }
    
    public static void deleteProfile(String name) {
        String base = "p54_" + name + "_";
        prefs.remove(base + "fb");
        prefs.remove(base + "pub");
        prefs.remove(base + "priv");
        
        List<String> names = getProfileNames();
        names.remove(name);
        saveProfileNames(names);
    }

    private static List<String> getProfileNames() {
        String raw = prefs.get(PROFILES_KEY, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    private static void saveProfileNames(List<String> names) {
        prefs.put(PROFILES_KEY, String.join(",", names));
    }
}

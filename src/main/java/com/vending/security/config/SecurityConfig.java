package com.vending.security.config;

public class SecurityConfig {
    // JWT Configuration
    public static final String JWT_SECRET = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJQcm9nZXR0byBQaXNzaXIiLCJuYW1lIjoiRWRvYXJkbyBHaW92YW5uaSBGcmFjY2hpYSIsIm1hdHJpY29sYSI6MjAwMTkzMDl9.V-9vaUbsee7Oc8gg7sdqfQEcqfhHxkRxW0QqopkLTlQ";
    public static final long JWT_EXPIRATION = 864_000_000; // 10 giorni
    
    // Password Configuration
    public static final int SALT_LENGTH = 16;
    public static final int HASH_ITERATIONS = 10000;
    public static final int KEY_LENGTH = 256;
    
    // MQTT Security
    public static final String MQTT_USERNAME = "20019309";
    public static final String MQTT_PASSWORD = "Pissir2024!";
    public static final boolean MQTT_REQUIRE_SSL = true;
    
    // API Security
    public static final String[] PUBLIC_ENDPOINTS = {
        // Pagine HTML pubbliche
        "/pages/client/machineSelection.html",
        "/pages/client/beverageInterface.html",
        
        // Endpoint API pubblici per le pagine client
        "/api/macchine",                    // GET - Lista di tutte le macchine
        "/api/macchine/*",                  // GET - Dettagli singola macchina
        "/api/macchine/*/stato",            // GET - Stato macchina
        "/api/macchine/istituto/*",         // GET - Macchine per istituto
        "/api/bevande",                     // GET - Lista bevande
        "/api/bevande/*",                   // GET - Dettagli bevanda
        "/api/istituti",                    // GET - Lista istituti
        "/api/istituti/*",                  // GET - Dettagli istituto
        
        // Endpoint per transazioni
        "/api/macchine/*/erogazione",       // POST - Erogazione bevanda
        "/api/macchine/*/credito",          // POST - Gestione credito
        
        // Endpoint di autenticazione
        "/api/auth/login",
        "/api/auth/register"
    };
    
    public static final String[] ADMIN_ENDPOINTS = {
        // Endpoint riservati agli amministratori
        "/api/admin/*",
        "/api/istituti/create",
        "/api/istituti/delete/*",
        "/api/macchine/create",
        "/api/macchine/delete/*",
        "/api/bevande/create",
        "/api/bevande/delete/*",
        "/api/manutenzione/*"
    };
}
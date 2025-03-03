package com.vending.iot.monitor;

import java.time.LocalDateTime;

public class Allarme {
    private String tipo;
    private String messaggio;
    private int severita;  // 1: bassa, 2: media, 3: alta
    private LocalDateTime timestamp;

    public Allarme(String tipo, String messaggio, int severita) {
        this.tipo = tipo;
        this.messaggio = messaggio;
        this.severita = severita;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getTipo() { return tipo; }
    public String getMessaggio() { return messaggio; }
    public int getSeverita() { return severita; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
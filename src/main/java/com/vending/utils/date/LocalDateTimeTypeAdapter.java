package com.vending.utils.date;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {
    // Formato italiano: giorno/mese/anno ore:minuti:secondi
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(formatter.format(value)); 
        }
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == null || in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        
        String dateTimeString = in.nextString();
        try {
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (Exception e) {
            // Prova a interpretarlo come formato ISO
            try {
                return LocalDateTime.parse(dateTimeString);
            } catch (Exception e2) {
                throw new IOException("Impossibile parsare la data: " + dateTimeString, e2);
            }
        }
}
}
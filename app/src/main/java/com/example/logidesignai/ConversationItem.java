package com.example.logidesignai;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConversationItem {

    private long id;
    private String title;
    private long timestamp;
    private String niche;
    private String lastMessage;

    public ConversationItem(long id, String title, long timestamp, String niche, String lastMessage) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.niche = niche;
        this.lastMessage = lastMessage;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null && !title.isEmpty() ? title : "Nueva conversación";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getFormattedRelativeTime() {
        if (timestamp <= 0) return "";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 0) diff = 0;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "Ahora";
        } else if (minutes < 60) {
            return "Hace " + minutes + " min";
        } else if (hours < 24) {
            return "Hace " + hours + (hours == 1 ? " hora" : " horas");
        } else if (days == 1) {
            return "Ayer";
        } else if (days < 7) {
            return "Hace " + days + " días";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("es", "ES"));
            return sdf.format(new Date(timestamp));
        }
    }
}

package com.fxz.artagent;

import java.util.List;

public class MessageBean {

    private String role;
    private String message;
    private boolean isImage;
    private boolean hasThemes;
    private List<String> themes;

    public List<String> getThemes() {
        return themes;
    }

    public MessageBean(String role, String message) {
        this(role, message, false, false);
    }

    public MessageBean(String role, String message, boolean isImage) {
        this(role, message, isImage, false);
    }

    public MessageBean(String role, String message, boolean isImage, boolean hasThemes) {
        this(role, message, isImage, hasThemes, null);
    }

    public MessageBean(String role, String message, boolean isImage, boolean hasThemes, List<String> themes) {
        this.role = role;
        this.message = message;
        this.isImage = isImage;
        this.hasThemes = hasThemes;
        this.themes = themes;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return role.equals("user");
    }

    public boolean isImage() {
        return isImage;
    }

    public boolean hasThemes() {
        return hasThemes;
    }
}
package com.fxz.artagent;

public class MessageBean {

    private String role;
    private String message;
    private boolean isImage;

    public MessageBean(String role, String message) {
        this.role = role;
        this.message = message;
        this.isImage = false; // 默认为非图片
    }

    public MessageBean(String role, String message, boolean isImage) { // 新添加的构造方法
        this.role = role;
        this.message = message;
        this.isImage = isImage;
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
}
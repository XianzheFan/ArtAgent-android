package com.fxz.artagent;

public class MessageBean {

    private String message;
    private boolean isRightLayout;

    public MessageBean(String message, boolean isRightLayout) {
        this.message = message;
        this.isRightLayout = isRightLayout;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRightLayout() {
        return isRightLayout;
    }
}

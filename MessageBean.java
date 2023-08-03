package com.fxz.artagent;

//public class MessageBean {
//
//    private String message;
//    private boolean isRightLayout;
//
//    public MessageBean(String message, boolean isRightLayout) {
//        this.message = message;
//        this.isRightLayout = isRightLayout;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public boolean isRightLayout() {
//        return isRightLayout;
//    }
//}

public class MessageBean {

    private String role;
    private String message;

    public MessageBean(String role, String message) {
        this.role = role;
        this.message = message;
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
}
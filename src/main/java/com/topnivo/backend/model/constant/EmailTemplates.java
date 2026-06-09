package com.topnivo.backend.model.constant;

import lombok.Getter;

public enum EmailTemplates {

    ACCOUNT_CREATION_CONFIRMATION("new-account-confirmation.html", "You have created an account with Topnivo"),
    FORGOT_PASSWORD_EMAIL("forgot-password.html", "You have generated a new password!"),
    ORDER_CONFIRMATION("order-confirmation.html", "Order confirmation");

    @Getter
    private final String template;
    @Getter
    private final String subject;

    EmailTemplates(String template, String subject) {
        this.template = template;
        this.subject = subject;
    }
}
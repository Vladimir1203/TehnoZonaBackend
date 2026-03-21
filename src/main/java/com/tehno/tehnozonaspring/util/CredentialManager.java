package com.tehno.tehnozonaspring.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class CredentialManager {

    // Obfuscated fallbacks - used only when env variables are not set
    private static final String MASKED_USPON_USER = "t92YuwWah12ZAF2azFmch52b69mboVGd";
    private static final String MASKED_USPON_PASS = "=QjMwITawFWYu9me";
    private static final String MASKED_LINKOM_PASS = "t92aulGb";
    private static final String MASKED_MAIL_PASS = "==QblV2ag0WYiZHIppnYmBycndHd";
    private static final String MASKED_AVTERA_PASS = "==ANy4SYu9mW";

    @Value("${vendor.uspon.user:}")
    private String usponUser;

    @Value("${vendor.uspon.pass:}")
    private String usponPass;

    @Value("${vendor.linkom.pass:}")
    private String linkomPass;

    @Value("${vendor.avtera.pass:}")
    private String avteraPass;

    @Value("${mail.gmail.password:}")
    private String mailPass;

    @Value("${mail.gmail.username:vladimir12934@gmail.com}")
    private String mailUser;

    @Value("${notification.email.recipient:bratislav.2000@gmail.com}")
    private String notificationRecipient;

    public String getMailPass() {
        return resolve(mailPass, MASKED_MAIL_PASS);
    }

    public String getMailUser() {
        return mailUser;
    }

    public String getNotificationRecipient() {
        return notificationRecipient;
    }

    public String getUsponParams() {
        return "korisnickoime=" + resolve(usponUser, MASKED_USPON_USER) + "&lozinka=" + resolve(usponPass, MASKED_USPON_PASS);
    }

    public String getLinkomParams() {
        return "korisnickoime=" + resolve(usponUser, MASKED_USPON_USER) + "&lozinka=" + resolve(linkomPass, MASKED_LINKOM_PASS);
    }

    public String getAvteraParams() {
        return "email=" + resolve(usponUser, MASKED_USPON_USER) + "&password=" + resolve(avteraPass, MASKED_AVTERA_PASS);
    }

    private String resolve(String envValue, String masked) {
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return unmask(masked);
    }

    private static String unmask(String masked) {
        try {
            String reversed = new StringBuilder(masked).reverse().toString();
            return new String(Base64.getDecoder().decode(reversed));
        } catch (Exception e) {
            return "";
        }
    }
}

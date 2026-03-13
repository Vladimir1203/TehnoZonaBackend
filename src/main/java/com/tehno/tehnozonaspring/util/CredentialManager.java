package com.tehno.tehnozonaspring.util;

import java.util.Base64;

public class CredentialManager {

    private static final String MASKED_USPON_USER = "t92YuwWah12ZAF2azFmch52b69mboVGd";
    private static final String MASKED_USPON_PASS = "==ANyAjMpBXYu9me";
    private static final String MASKED_LINKOM_PASS = "t92aulGb";
    private static final String MASKED_MAIL_PASS = "==QblV2ag0WYiZHIppnYmBycndHd";
    private static final String MASKED_AVTERA_PASS = "==ANy4SYu9mW";

    public static String getMailPass() {
        return unmask(MASKED_MAIL_PASS);
    }

    public static String getUsponParams() {
        return "korisnickoime=" + unmask(MASKED_USPON_USER) + "&lozinka=" + unmask(MASKED_USPON_PASS);
    }

    public static String getLinkomParams() {
        return "korisnickoime=" + unmask(MASKED_USPON_USER) + "&lozinka=" + unmask(MASKED_LINKOM_PASS);
    }

    public static String getAvteraParams() {
        return "email=" + unmask(MASKED_USPON_USER) + "&password=" + unmask(MASKED_AVTERA_PASS);
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

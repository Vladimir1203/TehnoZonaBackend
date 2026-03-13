package com.tehno.tehnozonaspring.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendErrorNotification(String vendorName, String errorMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("vladimir12934@gmail.com", "bratislav.2000@gmail.com");
            message.setSubject("ALARM: Feed Refresh Error - " + vendorName);
            message.setText(
                    "Došlo je do greške prilikom osvežavanja feed-a za " + vendorName + ".\n\nGreška: " + errorMessage);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    public void sendSuccessNotification(String vendorName, String hash) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("vladimir12934@gmail.com", "bratislav.2000@gmail.com");
            message.setSubject("✅ USPEH: Osvežen feed - " + vendorName.toUpperCase());

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

            String body = String.format("""
                    Sistem je uspešno osvežio podatke za prodavca: %s

                    Vreme: %s
                    Novi Hash: %s
                    Status: Uspešno upisano u bazu (XML tip)
                    Akcija: Prethodno stanje je arhivirano, stariji zapisi obrisani.

                    Pozdrav,
                    TehnoZona Bot 🤖
                    """, vendorName, timestamp, hash);

            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send success email for " + vendorName + ": " + e.getMessage());
        }
    }
}

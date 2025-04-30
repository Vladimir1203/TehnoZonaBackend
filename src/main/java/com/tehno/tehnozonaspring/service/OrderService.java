package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.OrderRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final JavaMailSender mailSender;

    @Autowired
    public OrderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderEmail(OrderRequest request) {
        String to = "bratislav.2000@gmail.com"; // primalac
        String subject = "Nova porudžbina od " + request.getIme() + " " + request.getPrezime();
        String body = generateEmailBody(request);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("vladimir12934@gmail.com"); // tvoja gmail adresa
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML

            try {
                mailSender.send(message);
            } catch (Exception e) {
                e.printStackTrace(); // ili log.error("Neuspešno slanje mejla", e);
                throw new RuntimeException("Slanje mejla nije uspelo. Pokušajte kasnije.");
            }        } catch (MessagingException e) {
            throw new RuntimeException("Greška prilikom slanja porudžbine: " + e.getMessage(), e);
        }
    }

    private String generateEmailBody(OrderRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("<h3>Nova porudžbina</h3>")
                .append("<p><strong>Ime i prezime:</strong> ").append(request.getIme()).append(" ").append(request.getPrezime()).append("</p>")
                .append("<p><strong>Email:</strong> ").append(request.getEmail()).append("</p>")
                .append("<p><strong>Adresa:</strong> ").append(request.getAdresa()).append("</p>")
                .append("<p><strong>Poštanski broj:</strong> ").append(request.getPostanskiBroj()).append("</p>")
                .append("<hr>")
                .append("<h4>Artikli:</h4>");

        List<Artikal> artikli = request.getArtikli();
        if (artikli != null && !artikli.isEmpty()) {
            sb.append("<ul>");
            for (Artikal artikal : artikli) {
                sb.append("<li>")
                        .append("<strong>").append(artikal.getNaziv()).append("</strong>")
                        .append(" (Šifra: ").append(artikal.getSifra()).append(") — ")
                        .append("Cena: ").append(artikal.getB2bcena()).append(" ").append(" — ")
                        .append("Kolicina: ").append(artikal.getKolicina()).append(" ")
                        .append("</li>");
            }
            sb.append("</ul>");
        } else {
            sb.append("<p><em>Nema dodatih artikala.</em></p>");
        }

        return sb.toString();
    }
}

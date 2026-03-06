package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify Your BidMart Account");
        message.setText("Welcome to BidMart! Please click the link below to verify your email:\n\n" + link);

        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Your BidMart Password");
        message.setText("You requested a password reset. Click the link below to set a new password:\n\n" +
                link + "\n\nIf you didn't request this, please ignore this email.");
        mailSender.send(message);
    }
}
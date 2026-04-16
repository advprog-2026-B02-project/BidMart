package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String link) {
        if (!mailEnabled) {
            log.warn("Email sending disabled. Verification link for {}: {}", to, link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Verify Your BidMart Account");
            message.setText("Welcome to BidMart! Please click the link below to verify your email:\n\n" + link);

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            log.info("Fallback verification link for {}: {}", to, link);
        }
    }

    @Async
    public void sendResetPasswordEmail(String to, String link) {
        if (!mailEnabled) {
            log.warn("Email sending disabled. Password reset link for {}: {}", to, link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Reset Your BidMart Password");
            message.setText("You requested a password reset. Click the link below to set a new password:\n\n"
                    + link + "\n\nIf you didn't request this, please ignore this email.");
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send reset password email to {}: {}", to, e.getMessage());
            log.info("Fallback reset password link for {}: {}", to, link);
        }
    }

}
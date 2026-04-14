package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetPasswordEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendResetPasswordEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

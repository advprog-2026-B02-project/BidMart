package id.ac.ui.cs.advprog.bidmart.backend.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmail() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetPasswordEmail() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendResetPasswordEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendVerificationEmail_Disabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        emailService.sendVerificationEmail("test@test.com", "http://link");

        verify(mailSender, org.mockito.Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendVerificationEmail_MailExceptionHandled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        doThrow(new MailSendException("fail")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetPasswordEmail_MailExceptionHandled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        doThrow(new MailSendException("fail")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendResetPasswordEmail("test@test.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetPasswordEmail_Disabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        emailService.sendResetPasswordEmail("test@test.com", "http://link");

        verify(mailSender, org.mockito.Mockito.never()).send(any(SimpleMailMessage.class));
    }
}

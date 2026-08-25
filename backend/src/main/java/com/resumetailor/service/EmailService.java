package com.resumetailor.service;

import com.resumetailor.config.EnvironmentConfig;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Properties;

public class EmailService {

    private final SesClient sesClient;
    private final EnvironmentConfig config;

    public EmailService() {
        this.config = EnvironmentConfig.getInstance();
        Region region = Region.of(config.getAwsRegion());
        this.sesClient = SesClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public void sendEmailWithAttachment(String recipientEmail, String pdfBase64, String coverLetterBase64, String subject, String body, String jobUrl) {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);
            message.setSubject(subject);
            message.setFrom(new InternetAddress(config.getSesSourceEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body + (jobUrl == null || jobUrl.isBlank() ? "" : "\n\nJob URL: " + jobUrl));
            multipart.addBodyPart(textPart);

            if (pdfBase64 != null && !pdfBase64.isBlank()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
                attachmentPart.setContent(pdfBytes, "application/pdf");
                attachmentPart.setFileName("Tailored_Resume.pdf");
                multipart.addBodyPart(attachmentPart);
            }

            if (coverLetterBase64 != null && !coverLetterBase64.isBlank()) {
                MimeBodyPart coverLetterPart = new MimeBodyPart();
                byte[] coverBytes = Base64.getDecoder().decode(coverLetterBase64);
                coverLetterPart.setContent(coverBytes, "application/pdf");
                coverLetterPart.setFileName("Tailored_Cover_Letter.pdf");
                multipart.addBodyPart(coverLetterPart);
            }

            message.setContent(multipart);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            ByteBuffer buffer = ByteBuffer.wrap(outputStream.toByteArray());

            SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder().data(SdkBytes.fromByteBuffer(buffer)).build())
                    .build();

            sesClient.sendRawEmail(rawEmailRequest);

        } catch (Exception e) {
            throw new RuntimeException("Email sending failed", e);
        }
    }
}

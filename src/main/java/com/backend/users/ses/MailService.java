package com.backend.users.ses;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest;

@Service
@Slf4j
public class MailService {
  private static final String WELCOME_TEMPLATE_PREFIX = "welcome-mail-";
  private static final String FORGOT_PASSWORD_TEMPLATE_PREFIX = "forgot-password-mail-";

  private final SesClient sesClient;
  private final String welcomeTemplateName;
  private final String forgotPasswordTemplateName;
  private final String senderEmail;
  private final String appDomain;

  public MailService(
      SesClient sesClient,
      @Value("${ENVIRONMENT}") String environment,
      @Value("${aws.ses.sender-email}") String senderEmail,
      @Value("${app.domain}") String appDomain) {
    this.sesClient = sesClient;
    this.welcomeTemplateName = WELCOME_TEMPLATE_PREFIX + environment;
    this.forgotPasswordTemplateName = FORGOT_PASSWORD_TEMPLATE_PREFIX + environment;
    this.senderEmail = senderEmail;
    this.appDomain = appDomain;
  }

  public Mono<Void> sendWelcomeMail(String mail) {
    return Mono.fromCallable(
            () -> {
              String username = mail.contains("@") ? mail.substring(0, mail.indexOf("@")) : mail;
              SendTemplatedEmailRequest request =
                  SendTemplatedEmailRequest.builder()
                      .source(senderEmail)
                      .destination(Destination.builder().toAddresses(mail).build())
                      .template(welcomeTemplateName)
                      .templateData("{\"username\":\"" + username + "\"}")
                      .build();

              sesClient.sendTemplatedEmail(request);
              log.info("Welcome email sent to {}", mail);
              return null;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("Failed to send welcome email to {}: {}", mail, e.getMessage()))
        .then();
  }

  public Mono<Void> sendResetPasswordMail(String mail, String token) {
    return Mono.fromCallable(
            () -> {
              String templateData =
                  String.format("{\"domain\":\"%s\",\"token\":\"%s\"}", appDomain, token);

              SendTemplatedEmailRequest request =
                  SendTemplatedEmailRequest.builder()
                      .source(senderEmail)
                      .destination(Destination.builder().toAddresses(mail).build())
                      .template(forgotPasswordTemplateName)
                      .templateData(templateData)
                      .build();

              sesClient.sendTemplatedEmail(request);
              log.info("Reset password email sent to {}", mail);
              return null;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            e -> log.error("Failed to send reset password email to {}: {}", mail, e.getMessage()))
        .then();
  }
}

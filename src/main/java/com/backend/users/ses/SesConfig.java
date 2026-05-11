package com.backend.users.ses;

import static com.backend.core.utils.AppProfiles.CLOUD;
import static com.backend.core.utils.AppProfiles.LOCAL_DEV;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

  @Bean
  @Profile(LOCAL_DEV)
  public SesClient localdevSesClient(@Value("${aws.ses.localstack-endpoint}") String endpoint) {
    return SesClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.AP_SOUTHEAST_1)
        .build();
  }

  @Bean
  @Profile(CLOUD)
  public SesClient sesClient() {
    return SesClient.create();
  }
}

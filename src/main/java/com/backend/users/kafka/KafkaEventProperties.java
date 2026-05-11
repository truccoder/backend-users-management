package com.backend.users.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-properties")
public class KafkaEventProperties {
  private String followTopicName;
  private String unfollowTopicName;
  private String blockTopicName;
  private String unblockTopicName;
  private String deadLetterTopicName;
}

package com.backend.users.kafka;

import java.util.Objects;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backend.users.dtos.BaseEvent;
import com.backend.users.dtos.BlockPayloadDto;
import com.backend.users.dtos.FollowPayloadDto;
import com.backend.users.dtos.UnblockPayloadDto;
import com.backend.users.dtos.UnfollowPayloadDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisher {
  @Value("${ENVIRONMENT}")
  private String environment;

  private final KafkaEventProperties eventProperties;
  private final KafkaSender<String, BaseEvent> kafkaSender;

  public Mono<Void> sendFollowEvent(FollowPayloadDto payload) {
    BaseEvent event = new BaseEvent(environment, payload);
    return sendEvent(event, eventProperties.getFollowTopicName());
  }

  public Mono<Void> sendUnfollowEvent(UnfollowPayloadDto payload) {
    BaseEvent event = new BaseEvent(environment, payload);
    return sendEvent(event, eventProperties.getUnfollowTopicName());
  }

  public Mono<Void> sendBlockEvent(BlockPayloadDto payload) {
    BaseEvent event = new BaseEvent(environment, payload);
    return sendEvent(event, eventProperties.getBlockTopicName());
  }

  public Mono<Void> sendUnblockEvent(UnblockPayloadDto payload) {
    BaseEvent event = new BaseEvent(environment, payload);
    return sendEvent(event, eventProperties.getUnblockTopicName());
  }

  private Mono<Void> sendEvent(BaseEvent event, String topic) {
    String key = String.valueOf(event.getPayload().getUserId());
    ProducerRecord<String, BaseEvent> producerRecord = new ProducerRecord<>(topic, key, event);
    SenderRecord<String, BaseEvent, String> senderRecord = SenderRecord.create(producerRecord, key);

    return kafkaSender
        .send(Mono.just(senderRecord))
        .single()
        .flatMap(
            result -> {
              if (Objects.nonNull(result.exception())) {
                log.error(
                    "Failed to send event to topic '{}' with key '{}': {}",
                    topic,
                    key,
                    result.exception().getMessage());
                return sendToDeadLetterTopic(event, key, result.exception());
              }
              log.info(
                  "Successfully sent event to topic '{}' with key '{}' at offset {}",
                  topic,
                  key,
                  result.recordMetadata().offset());
              return Mono.empty();
            })
        .then();
  }

  private Mono<Void> sendToDeadLetterTopic(BaseEvent event, String key, Throwable originalError) {
    String deadLetterTopic = eventProperties.getDeadLetterTopicName();
    ProducerRecord<String, BaseEvent> dlqRecord = new ProducerRecord<>(deadLetterTopic, key, event);
    SenderRecord<String, BaseEvent, String> senderRecord = SenderRecord.create(dlqRecord, key);

    return kafkaSender
        .send(Mono.just(senderRecord))
        .single()
        .doOnNext(
            result ->
                log.info(
                    "Sent failed event to dead letter topic '{}' with key '{}', original error: {}",
                    deadLetterTopic,
                    key,
                    originalError.getMessage()))
        .doOnError(
            e ->
                log.error(
                    "Failed to send event to dead letter topic '{}' with key '{}': {}",
                    deadLetterTopic,
                    key,
                    e.getMessage()))
        .then();
  }
}

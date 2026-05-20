package com.backend.users.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.backend.users.dtos.BaseEvent;
import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.enums.FriendRequestStatus;
import com.backend.users.graph.UserNode;
import com.backend.users.repositories.FriendRequestRepository;
import com.backend.users.repositories.UserNodeRepository;
import com.backend.users.repositories.UserRepository;
import com.backend.users.services.KeycloakService;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestSecurityConfig.class})
public abstract class BaseTest {
  protected static final String TOPIC_FOLLOWS = "follows";
  protected static final String TOPIC_UNFOLLOWS = "unfollows";
  protected static final String TOPIC_BLOCKS = "blocks";
  protected static final String TOPIC_UNBLOCKS = "unblocks";
  protected static final String TOPIC_DEAD_LETTERS = "deadLetters";

  @Autowired protected WebTestClient webTestClient;
  @Autowired protected UserRepository userRepository;
  @Autowired protected FriendRequestRepository friendRequestRepository;
  @Autowired protected UserNodeRepository userNodeRepository;
  @Autowired protected TransactionalOperator transactionalOperator;

  @Autowired
  @Qualifier("reactiveStringRedisTemplate")
  protected ReactiveRedisTemplate<String, String> redisTemplate;

  @MockBean protected KeycloakService keycloakService;

  protected static Consumer<String, BaseEvent> kafkaConsumer;

  @Container
  static final KafkaContainer kafkaContainer =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
          .withStartupTimeout(Duration.ofSeconds(60))
          .withStartupAttempts(2);

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
  }

  @BeforeAll
  static void setUpKafka() {
    createTopic(TOPIC_FOLLOWS);
    createTopic(TOPIC_UNFOLLOWS);
    createTopic(TOPIC_BLOCKS);
    createTopic(TOPIC_UNBLOCKS);
    createTopic(TOPIC_DEAD_LETTERS);

    Map<String, Object> consumerProps = new HashMap<>();
    consumerProps.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class.getName());

    DefaultKafkaConsumerFactory<String, BaseEvent> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    kafkaConsumer = consumerFactory.createConsumer();
    kafkaConsumer.subscribe(
        Arrays.asList(
            TOPIC_FOLLOWS, TOPIC_UNFOLLOWS, TOPIC_BLOCKS, TOPIC_UNBLOCKS, TOPIC_DEAD_LETTERS));
  }

  @AfterAll
  static void tearDownKafkaConsumer() {
    if (Objects.nonNull(kafkaConsumer)) {
      kafkaConsumer.close();
    }
  }

  @BeforeEach
  void cleanUp() {
    if (Objects.nonNull(kafkaConsumer)) {
      kafkaConsumer.poll(Duration.ofMillis(100));
    }

    transactionalOperator
        .transactional(friendRequestRepository.deleteAll().then(userRepository.deleteAll()))
        .block();
    userNodeRepository.deleteAll().block();
    redisTemplate
        .getConnectionFactory()
        .getReactiveConnection()
        .serverCommands()
        .flushAll()
        .block();
  }

  private static void createTopic(String topicName) {
    Map<String, Object> config =
        Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    try (AdminClient admin = AdminClient.create(config)) {
      NewTopic topic = new NewTopic(topicName, 1, (short) 1);
      admin.createTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create topic: " + topicName, e);
    }
  }

  protected <T> T consumeKafkaMessage(String topic, Class<T> payloadClass) {
    List<ConsumerRecord<String, BaseEvent>> records = new ArrayList<>();
    long deadline = System.currentTimeMillis() + Duration.ofSeconds(5).toMillis();

    while (records.isEmpty() && System.currentTimeMillis() < deadline) {
      ConsumerRecords<String, BaseEvent> consumerRecords =
          kafkaConsumer.poll(Duration.ofMillis(100));
      consumerRecords.records(topic).forEach(records::add);
    }

    assertFalse(records.isEmpty());

    BaseEvent event = records.get(0).value();
    assertNotNull(event);
    assertNotNull(event.getPayload());
    assertTrue(payloadClass.isInstance(event.getPayload()));

    return payloadClass.cast(event.getPayload());
  }

  protected UserEntity createUser(String email) {
    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID().toString());
    user.setEmail(email);
    user.setFullName("Test User");
    return transactionalOperator.transactional(userRepository.save(user)).block();
  }

  protected UserNode createUserNode(String userId, String email) {
    UserNode node = new UserNode();
    node.setId(userId);
    node.setFullName("Test User");
    return userNodeRepository.save(node).block();
  }

  protected FriendRequestEntity createFriendRequest(
      String requesterId, String addresseeId, FriendRequestStatus status) {
    FriendRequestEntity request = new FriendRequestEntity();
    request.setId(UUID.randomUUID().toString());
    request.setRequesterId(requesterId);
    request.setAddresseeId(addresseeId);
    request.setStatus(status);
    return transactionalOperator.transactional(friendRequestRepository.save(request)).block();
  }

  protected String generateToken(UserEntity user) {
    return TestSecurityConfig.generateToken(user);
  }
}

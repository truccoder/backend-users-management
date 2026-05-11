package com.backend.users.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.backend.core.dtos.UserDto;
import com.backend.core.dtos.ValidateTokenRequestDto;
import com.backend.core.dtos.ValidateTokenResponseDto;
import com.backend.core.security.UserClient;
import com.backend.users.dtos.BaseEvent;
import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.entities.PasswordResetTokenEntity;
import com.backend.users.entities.RefreshTokenEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.enums.FriendRequestStatus;
import com.backend.users.enums.JwtPayloadFields;
import com.backend.users.graph.UserNode;
import com.backend.users.repositories.FriendRequestRepository;
import com.backend.users.repositories.PasswordResetTokenRepository;
import com.backend.users.repositories.RefreshTokenRepository;
import com.backend.users.repositories.UserNodeRepository;
import com.backend.users.repositories.UserRepository;
import com.backend.users.utils.JwtUtil;

import reactor.core.publisher.Mono;

@Testcontainers // manage containers lifecycle
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import({TestContainersConfig.class})
public abstract class BaseTest {
  protected static final String DEFAULT_PASSWORD = "password123";
  protected static final String TOPIC_FOLLOWS = "follows";
  protected static final String TOPIC_UNFOLLOWS = "unfollows";
  protected static final String TOPIC_BLOCKS = "blocks";
  protected static final String TOPIC_UNBLOCKS = "unblocks";
  protected static final String TOPIC_DEAD_LETTERS = "deadLetters";

  @Autowired protected WebTestClient webTestClient;
  @Autowired protected UserRepository userRepository;
  @Autowired protected FriendRequestRepository friendRequestRepository;
  @Autowired protected UserNodeRepository userNodeRepository;
  @Autowired protected RefreshTokenRepository refreshTokenRepository;
  @Autowired protected PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected JwtUtil jwtUtil;
  @Autowired protected TransactionalOperator transactionalOperator;

  @Autowired
  @Qualifier("reactiveStringRedisTemplate")
  protected ReactiveRedisTemplate<String, String> redisTemplate;

  @MockBean protected UserClient userClient;

  protected static Consumer<String, BaseEvent> kafkaConsumer;

  // Automatically started before test, stop after all tests complete
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
    // Poll and discard any remaining messages before each test
    if (Objects.nonNull(kafkaConsumer)) {
      kafkaConsumer.poll(Duration.ofMillis(100));
    }

    // clear all
    transactionalOperator
        .transactional(
            refreshTokenRepository
                .deleteAll()
                .then(passwordResetTokenRepository.deleteAll())
                .then(friendRequestRepository.deleteAll())
                .then(userRepository.deleteAll()))
        .block();
    userNodeRepository.deleteAll().block();
    redisTemplate
        .getConnectionFactory()
        .getReactiveConnection()
        .serverCommands()
        .flushAll()
        .block();

    // mock user token
    Mockito.when(userClient.validateToken(Mockito.any()))
        .thenAnswer(
            invocation -> {
              ValidateTokenRequestDto req = invocation.getArgument(0);
              Map<String, Object> extractPayload = jwtUtil.extractPayload(req.getToken());
              UserDto user =
                  new UserDto(
                      Long.valueOf(extractPayload.get(JwtPayloadFields.ID.getName()).toString()),
                      extractPayload.get(JwtPayloadFields.EMAIL.getName()).toString());

              ValidateTokenResponseDto res =
                  new ValidateTokenResponseDto(
                      true, Date.from(OffsetDateTime.now().plusHours(1).toInstant()), user);
              return Mono.just(res);
            });
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
    System.out.println(">>payload " + event.getPayload());
    assertTrue(payloadClass.isInstance(event.getPayload()));

    return payloadClass.cast(event.getPayload());
  }

  protected UserEntity createUser(String email) {
    return createUser(email, DEFAULT_PASSWORD);
  }

  protected UserEntity createUser(String email, String password) {
    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    return transactionalOperator.transactional(userRepository.save(user)).block();
  }

  protected UserEntity findUser(Long id) {
    return transactionalOperator.transactional(userRepository.findById(id)).block();
  }

  protected RefreshTokenEntity createRefreshToken(UserEntity user, OffsetDateTime expiresAt) {
    RefreshTokenEntity token = new RefreshTokenEntity();
    token.setToken(java.util.UUID.randomUUID().toString());
    token.setUserId(user.getId());
    token.setExpiresAt(expiresAt);
    return transactionalOperator.transactional(refreshTokenRepository.save(token)).block();
  }

  protected PasswordResetTokenEntity createPasswordResetToken(
      UserEntity user, OffsetDateTime expiresAt) {
    PasswordResetTokenEntity token = new PasswordResetTokenEntity();
    token.setToken(java.util.UUID.randomUUID().toString());
    token.setUserId(user.getId());
    token.setExpiresAt(expiresAt);
    return transactionalOperator.transactional(passwordResetTokenRepository.save(token)).block();
  }

  protected PasswordResetTokenEntity findPasswordResetToken(String token) {
    return transactionalOperator
        .transactional(passwordResetTokenRepository.findByToken(token))
        .block();
  }

  protected UserNode createUserNode(Long userId, String email) {
    UserNode node = new UserNode();
    node.setId(userId);
    node.setEmail(email);
    return userNodeRepository.save(node).block();
  }

  protected FriendRequestEntity createFriendRequest(
      Long requesterId, Long addresseeId, FriendRequestStatus status) {
    FriendRequestEntity request = new FriendRequestEntity();
    request.setRequesterId(requesterId);
    request.setAddresseeId(addresseeId);
    request.setStatus(status);
    return transactionalOperator.transactional(friendRequestRepository.save(request)).block();
  }

  protected String generateToken(UserEntity user) {
    return jwtUtil.generateToken(user);
  }
}

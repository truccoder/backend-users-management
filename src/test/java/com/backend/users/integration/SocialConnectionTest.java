package com.backend.users.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.backend.users.dtos.BlockPayloadDto;
import com.backend.users.dtos.UnblockPayloadDto;
import com.backend.users.entities.UserEntity;
import com.backend.users.graph.BlocksRelationship;
import com.backend.users.graph.FollowsRelationship;
import com.backend.users.graph.UserNode;

class SocialConnectionTest extends BaseTest {
  private UserEntity userA;
  private UserEntity userB;
  private UserEntity userC;
  private UserNode nodeA;
  private UserNode nodeB;
  private UserNode nodeC;

  @BeforeEach
  void setUp() {
    userA = createUser("userA@test.com");
    userB = createUser("userB@test.com");
    userC = createUser("userC@test.com");

    nodeA = createUserNode(userA.getId(), userA.getEmail());
    nodeB = createUserNode(userB.getId(), userB.getEmail());
    nodeC = createUserNode(userC.getId(), userC.getEmail());
  }

  @Nested
  class FollowUserTests {
    @Test
    void shouldFollowUser() {
      webTestClient
          .post()
          .uri("/v1/api/social/follow/{followedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    void shouldReturn401ForUnauthenticatedFollowRequest() {
      webTestClient
          .post()
          .uri("/v1/api/social/follow/{followedId}", userB.getId())
          .exchange()
          .expectStatus()
          .isUnauthorized();
    }
  }

  @Nested
  class UnfollowUserTests {
    @Test
    void shouldUnfollowUser() {
      webTestClient
          .post()
          .uri("/v1/api/social/unfollow/{followedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    void shouldHandleUnfollowWhenNotFollowing() {
      webTestClient
          .post()
          .uri("/v1/api/social/unfollow/{followedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();
    }
  }

  @Nested
  class GetFollowingFollowersTests {
    @BeforeEach
    void setupFollowRelationships() {
      nodeA.setFollowing(new ArrayList<>());
      FollowsRelationship followsB = new FollowsRelationship();
      followsB.setFollowedUser(nodeB);
      followsB.setCreatedAt(OffsetDateTime.now());
      nodeA.getFollowing().add(followsB);

      FollowsRelationship followsC = new FollowsRelationship();
      followsC.setFollowedUser(nodeC);
      followsC.setCreatedAt(OffsetDateTime.now());
      nodeA.getFollowing().add(followsC);

      userNodeRepository.save(nodeA).block();
    }

    @Test
    void shouldGetFollowingList() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/v1/api/social/following")
                      .queryParam("offset", 0)
                      .queryParam("pageSize", 10)
                      .build())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.items")
          .isArray()
          .jsonPath("$.totalElements")
          .isEqualTo(2);
    }

    @Test
    void shouldGetFollowersList() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/v1/api/social/followers")
                      .queryParam("offset", 0)
                      .queryParam("pageSize", 10)
                      .build())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.items")
          .isArray();
    }
  }

  @Nested
  class BlockUserTests {
    @Test
    void shouldBlockUser() {
      webTestClient
          .post()
          .uri("/v1/api/social/block/{blockedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();

      BlockPayloadDto payload = consumeKafkaMessage(TOPIC_BLOCKS, BlockPayloadDto.class);
      assertEquals(userA.getId(), payload.getUserId());
      assertEquals(userB.getId(), payload.getBlockedId());
    }

    @Test
    void shouldUnblockUser() {
      webTestClient
          .post()
          .uri("/v1/api/social/unblock/{blockedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();

      UnblockPayloadDto payload = consumeKafkaMessage(TOPIC_UNBLOCKS, UnblockPayloadDto.class);
      assertEquals(userA.getId(), payload.getUserId());
      assertEquals(userB.getId(), payload.getBlockedId());
    }
  }

  @Nested
  class GetBlockedUsersTests {
    @BeforeEach
    void setupBlockRelationships() {
      nodeA.setBlocked(new ArrayList<>());
      BlocksRelationship blocksB = new BlocksRelationship();
      blocksB.setBlockedUser(nodeB);
      blocksB.setCreatedAt(OffsetDateTime.now());
      nodeA.getBlocked().add(blocksB);

      userNodeRepository.save(nodeA).block();
    }

    @Test
    void shouldGetBlockedUsersList() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/v1/api/social/blocked")
                      .queryParam("offset", 0)
                      .queryParam("pageSize", 10)
                      .build())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.items")
          .isArray();
    }
  }

  @Nested
  class SecurityTests {
    @Test
    void shouldReturn401ForUnauthenticatedRequests() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/v1/api/social/following")
                      .queryParam("offset", 0)
                      .queryParam("pageSize", 10)
                      .build())
          .exchange()
          .expectStatus()
          .isUnauthorized();

      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/v1/api/social/blocked")
                      .queryParam("offset", 0)
                      .queryParam("pageSize", 10)
                      .build())
          .exchange()
          .expectStatus()
          .isUnauthorized();
    }
  }
}

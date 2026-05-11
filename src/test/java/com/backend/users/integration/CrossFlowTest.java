package com.backend.users.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.backend.core.dtos.UserDto;
import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.enums.FriendRequestStatus;
import com.backend.users.graph.FollowsRelationship;
import com.backend.users.graph.UserNode;

class CrossFlowTest extends BaseTest {
  private UserEntity userA;
  private UserEntity userB;
  private UserEntity userC;
  private UserEntity userD;
  private UserNode nodeA;
  private UserNode nodeB;
  private UserNode nodeC;
  private UserNode nodeD;

  @BeforeEach
  void setUp() {
    userA = createUser("userA@test.com");
    userB = createUser("userB@test.com");
    userC = createUser("userC@test.com");
    userD = createUser("userD@test.com");

    nodeA = createUserNode(userA.getId(), userA.getEmail());
    nodeB = createUserNode(userB.getId(), userB.getEmail());
    nodeC = createUserNode(userC.getId(), userC.getEmail());
    nodeD = createUserNode(userD.getId(), userD.getEmail());
  }

  @Nested
  class FriendRequestStateTransitionTests {
    @Test
    void shouldTransitionPendingToAccepted() {
      FriendRequestEntity pending =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", pending.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isOk();

      FriendRequestEntity updated = friendRequestRepository.findById(pending.getId()).block();
      assertThat(updated.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
    }

    @Test
    void shouldTransitionPendingToRejected() {
      FriendRequestEntity pending =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/reject", pending.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isOk();

      FriendRequestEntity updated = friendRequestRepository.findById(pending.getId()).block();
      assertThat(updated.getStatus()).isEqualTo(FriendRequestStatus.REJECTED);
    }

    @Test
    void shouldTransitionPendingToCancelled() {
      FriendRequestEntity pending =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/cancel", pending.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();

      FriendRequestEntity updated = friendRequestRepository.findById(pending.getId()).block();
      assertThat(updated.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
    }

    @Test
    void shouldNotAllowTransitionFromAccepted() {
      FriendRequestEntity accepted =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.ACCEPTED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/reject", accepted.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/cancel", accepted.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldNotAllowTransitionFromRejected() {
      FriendRequestEntity rejected =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.REJECTED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", rejected.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldNotAllowTransitionFromCancelled() {
      FriendRequestEntity cancelled =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.CANCELLED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", cancelled.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }
  }

  @Nested
  class FriendSuggestionsTests {
    @BeforeEach
    void setupFriendshipGraph() {
      nodeA.setFriends(new HashSet<>());
      nodeA.getFriends().add(nodeB);

      nodeB.setFriends(new HashSet<>());
      nodeB.getFriends().add(nodeC);
      nodeB.getFriends().add(nodeD);

      userNodeRepository.save(nodeA).block();
      userNodeRepository.save(nodeB).block();
    }

    @Test
    void shouldReturnFriendOfFriendsAsSuggestions() {
      webTestClient
          .get()
          .uri("/v1/api/friendships/suggestions")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(UserDto.class)
          .consumeWith(
              response -> {
                var suggestions = response.getResponseBody();
                assertThat(suggestions).isNotNull();
                var suggestionIds = suggestions.stream().map(UserDto::getId).toList();
                assertThat(suggestionIds).doesNotContain(userA.getId());
                assertThat(suggestionIds).doesNotContain(userB.getId());
              });
    }
  }

  @Nested
  class FollowRelationshipTests {
    @Test
    void shouldCreateFollowRelationship() {
      webTestClient
          .post()
          .uri("/v1/api/social/follow/{followedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    @DisplayName("Should remove follow relationship via Kafka")
    void shouldRemoveFollowRelationship() {
      nodeA.setFollowing(new ArrayList<>());
      FollowsRelationship followsB = new FollowsRelationship();
      followsB.setFollowedUser(nodeB);
      followsB.setCreatedAt(OffsetDateTime.now());
      nodeA.getFollowing().add(followsB);
      userNodeRepository.save(nodeA).block();

      webTestClient
          .post()
          .uri("/v1/api/social/unfollow/{followedId}", userB.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .exchange()
          .expectStatus()
          .isOk();
    }
  }
}

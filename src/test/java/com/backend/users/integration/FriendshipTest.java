package com.backend.users.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.backend.users.dtos.SendFriendRequestDto;
import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.enums.FriendRequestStatus;

class FriendshipTest extends BaseTest {
  private UserEntity userA;
  private UserEntity userB;
  private UserEntity userC;

  @BeforeEach
  void setUp() {
    userA = createUser("userA@test.com");
    userB = createUser("userB@test.com");
    userC = createUser("userC@test.com");

    createUserNode(userA.getId(), userA.getEmail());
    createUserNode(userB.getId(), userB.getEmail());
    createUserNode(userC.getId(), userC.getEmail());
  }

  @Nested
  class SendFriendRequestTests {
    @Test
    void shouldSendFriendRequestSuccess() {
      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(userB.getId());

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      FriendRequestEntity savedRequest =
          friendRequestRepository
              .findByIdAndStatus(userA.getId(), userB.getId(), FriendRequestStatus.PENDING.name())
              .block();

      assertThat(savedRequest).isNotNull();
      assertThat(savedRequest.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
      assertThat(savedRequest.getRequesterId()).isEqualTo(userA.getId());
      assertThat(savedRequest.getAddresseeId()).isEqualTo(userB.getId());
    }

    @Test
    public void shouldRejectSendingRequestToSelf() {
      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(userA.getId());

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectWhenAlreadyFriends() {
      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.ACCEPTED);

      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(userB.getId());

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectDuplicatePendingRequest() {
      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(userB.getId());

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldReturn404ForNonExistentAddressee() {
      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(99999L);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isNotFound();
    }

    @Test
    void shouldRejectBidirectionalPendingRequest() {
      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      SendFriendRequestDto request = new SendFriendRequestDto();
      request.setAddresseeId(userA.getId());

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }
  }

  @Nested
  class AcceptFriendRequestTests {
    @Test
    void shouldAcceptFriendRequest() {
      FriendRequestEntity pendingRequest =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", pendingRequest.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isOk();

      FriendRequestEntity updatedRequest =
          friendRequestRepository.findById(pendingRequest.getId()).block();

      assertThat(updatedRequest.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
    }

    @Test
    void shouldReturn404ForNonExistentRequest() {
      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", 99999L)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isNotFound();
    }

    @Test
    void shouldRejectAcceptingAlreadyAcceptedRequest() {
      FriendRequestEntity acceptedRequest =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.ACCEPTED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", acceptedRequest.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectAcceptingRejectedRequest() {
      FriendRequestEntity rejectedRequest =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.REJECTED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", rejectedRequest.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectAcceptingCancelledRequest() {
      FriendRequestEntity cancelledRequest =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.CANCELLED);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", cancelledRequest.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldForbidAcceptingAsWrongUser() {
      FriendRequestEntity pendingRequest =
          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);

      webTestClient
          .post()
          .uri("/v1/api/friendships/requests/{requestId}/accept", pendingRequest.getId())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userC))
          .exchange()
          .expectStatus()
          .isForbidden();
    }
  }
  //
  //  @Nested
  //  @DisplayName("Reject Friend Request")
  //  class RejectFriendRequestTests {
  //
  //    @Test
  //    @DisplayName("FR-HP-03: Should successfully reject friend request")
  //    void shouldRejectFriendRequest() {
  //      FriendRequestEntity pendingRequest =
  //          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .post()
  //          .uri("/v1/api/friendships/requests/{requestId}/reject", pendingRequest.getId())
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
  //          .exchange()
  //          .expectStatus()
  //          .isOk();
  //
  //      FriendRequestEntity updatedRequest =
  //          friendRequestRepository.findById(pendingRequest.getId()).block();
  //
  //      assertThat(updatedRequest.getStatus()).isEqualTo(FriendRequestStatus.REJECTED);
  //    }
  //
  //    @Test
  //    @DisplayName("FR-EC-10: Should forbid requester from rejecting")
  //    void shouldForbidRequesterFromRejecting() {
  //      FriendRequestEntity pendingRequest =
  //          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .post()
  //          .uri("/v1/api/friendships/requests/{requestId}/reject", pendingRequest.getId())
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
  //          .exchange()
  //          .expectStatus()
  //          .isForbidden();
  //    }
  //  }
  //
  //  @Nested
  //  @DisplayName("Cancel Friend Request")
  //  class CancelFriendRequestTests {
  //
  //    @Test
  //    @DisplayName("FR-HP-04: Should successfully cancel friend request")
  //    void shouldCancelFriendRequest() {
  //      FriendRequestEntity pendingRequest =
  //          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .post()
  //          .uri("/v1/api/friendships/requests/{requestId}/cancel", pendingRequest.getId())
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
  //          .exchange()
  //          .expectStatus()
  //          .isOk();
  //
  //      FriendRequestEntity updatedRequest =
  //          friendRequestRepository.findById(pendingRequest.getId()).block();
  //
  //      assertThat(updatedRequest.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
  //    }
  //
  //    @Test
  //    @DisplayName("FR-EC-11: Should forbid addressee from cancelling")
  //    void shouldForbidAddresseeFromCancelling() {
  //      FriendRequestEntity pendingRequest =
  //          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .post()
  //          .uri("/v1/api/friendships/requests/{requestId}/cancel", pendingRequest.getId())
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
  //          .exchange()
  //          .expectStatus()
  //          .isForbidden();
  //    }
  //
  //    @Test
  //    @DisplayName("FR-EC-12: Should reject cancelling non-pending request")
  //    void shouldRejectCancellingNonPendingRequest() {
  //      FriendRequestEntity acceptedRequest =
  //          createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.ACCEPTED);
  //
  //      webTestClient
  //          .post()
  //          .uri("/v1/api/friendships/requests/{requestId}/cancel", acceptedRequest.getId())
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
  //          .exchange()
  //          .expectStatus()
  //          .isBadRequest();
  //    }
  //  }
  //
  //  @Nested
  //  @DisplayName("Get Friend Requests")
  //  class GetFriendRequestsTests {
  //
  //    @Test
  //    @DisplayName("FR-HP-05: Should get pending friend requests")
  //    void shouldGetPendingFriendRequests() {
  //      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //      createFriendRequest(userC.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .get()
  //          .uri("/v1/api/friendships/requests/pending")
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
  //          .exchange()
  //          .expectStatus()
  //          .isOk()
  //          .expectBodyList(FriendRequestResponseDto.class)
  //          .hasSize(2);
  //    }
  //
  //    @Test
  //    @DisplayName("FR-HP-06: Should get sent friend requests")
  //    void shouldGetSentFriendRequests() {
  //      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //      createFriendRequest(userA.getId(), userC.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .get()
  //          .uri("/v1/api/friendships/requests/sent")
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userA))
  //          .exchange()
  //          .expectStatus()
  //          .isOk()
  //          .expectBodyList(FriendRequestResponseDto.class)
  //          .hasSize(2);
  //    }
  //
  //    @Test
  //    @DisplayName("Should only return pending requests, not accepted/rejected")
  //    void shouldOnlyReturnPendingRequests() {
  //      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //      createFriendRequest(userC.getId(), userB.getId(), FriendRequestStatus.ACCEPTED);
  //
  //      webTestClient
  //          .get()
  //          .uri("/v1/api/friendships/requests/pending")
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
  //          .exchange()
  //          .expectStatus()
  //          .isOk()
  //          .expectBodyList(FriendRequestResponseDto.class)
  //          .hasSize(1);
  //    }
  //  }
  //
  //  @Nested
  //  @DisplayName("Security Tests")
  //  class SecurityTests {
  //
  //    @Test
  //    @DisplayName("SEC-01: Should return 401 for unauthenticated request")
  //    void shouldReturn401ForUnauthenticatedRequest() {
  //      webTestClient
  //          .get()
  //          .uri("/v1/api/friendships/requests/pending")
  //          .exchange()
  //          .expectStatus()
  //          .isUnauthorized();
  //    }
  //
  //    @Test
  //    @DisplayName("SEC-02: Should only return own pending requests")
  //    void shouldOnlyReturnOwnPendingRequests() {
  //      createFriendRequest(userA.getId(), userB.getId(), FriendRequestStatus.PENDING);
  //      createFriendRequest(userA.getId(), userC.getId(), FriendRequestStatus.PENDING);
  //
  //      webTestClient
  //          .get()
  //          .uri("/v1/api/friendships/requests/pending")
  //          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(userB))
  //          .exchange()
  //          .expectStatus()
  //          .isOk()
  //          .expectBodyList(FriendRequestResponseDto.class)
  //          .hasSize(1)
  //          .consumeWith(
  //              response -> {
  //                FriendRequestResponseDto dto = response.getResponseBody().get(0);
  //                assertThat(dto.getAddresseeId()).isEqualTo(userB.getId());
  //              });
  //    }
  //  }
}

package com.backend.users.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.entities.PendingFriendRequestProjection;
import com.backend.users.entities.SentFriendRequestProjection;
import com.backend.users.enums.FriendRequestStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FriendRequestRepository extends R2dbcRepository<FriendRequestEntity, String> {
  @Query(
      """
        SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
        FROM t_friend_requests fr
        WHERE (
            (fr.requester_id = :requesterId AND fr.addressee_id = :addresseeId)
            OR
            (fr.requester_id = :addresseeId AND fr.addressee_id = :requesterId)
        )
        AND fr.status = 'ACCEPTED'
      """)
  Mono<Boolean> areFriends(
      @Param("requesterId") String requesterId, @Param("addresseeId") String addresseeId);

  @Query(
      """
        SELECT * FROM t_friend_requests fr
        WHERE fr.status = :status
        AND (
            (fr.requester_id = :requesterId AND fr.addressee_id = :addresseeId)
            OR
            (fr.requester_id = :addresseeId AND fr.addressee_id = :requesterId)
        )
      """)
  Mono<FriendRequestEntity> findByIdAndStatus(
      @Param("requesterId") String requesterId,
      @Param("addresseeId") String addresseeId,
      @Param("status") String status);

  @Query(
      """
    SELECT
        fr.id,
        fr.requester_id,
        u.full_name  AS requester_full_name,
        u.profile_picture_url AS requester_profile_picture_url,
        fr.status,
        fr.created_at
    FROM t_friend_requests fr
    JOIN t_users u ON u.id = fr.requester_id
    WHERE fr.addressee_id = :userId
      AND fr.status = :status
    """)
  Flux<PendingFriendRequestProjection> findPendingFriendRequests(
      String userId, FriendRequestStatus status);

  @Query(
      """
    SELECT
        fr.id,
        fr.addressee_id,
        u.full_name  AS addressee_full_name,
        u.profile_picture_url AS addressee_profile_picture_url,
        fr.status,
        fr.created_at
    FROM t_friend_requests fr
    JOIN t_users u ON u.id = fr.addressee_id
    WHERE fr.requester_id = :userId
      AND fr.status = :status
    """)
  Flux<SentFriendRequestProjection> findSentFriendRequests(
      String userId, FriendRequestStatus status);
}

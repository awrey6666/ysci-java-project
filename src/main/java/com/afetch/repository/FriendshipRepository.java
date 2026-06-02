package com.afetch.repository;

import com.afetch.domain.entity.Friendship;
import com.afetch.domain.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.status = :status AND f.addressee.id = :userId")
    List<Friendship> findPendingForUser(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.status = :status AND f.requester.id = :userId")
    List<Friendship> findPendingOutgoingForUser(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.status = com.afetch.domain.enums.FriendshipStatus.ACCEPTED " +
           "AND (f.requester.id = :userId OR f.addressee.id = :userId)")
    List<Friendship> findAcceptedForUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE f.status = com.afetch.domain.enums.FriendshipStatus.ACCEPTED " +
           "AND ((f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a))")
    boolean areFriends(@Param("a") Long userA, @Param("b") Long userB);

    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.requester.id = :u1 AND f.addressee.id = :u2) OR (f.requester.id = :u2 AND f.addressee.id = :u1))")
    Optional<Friendship> findBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}

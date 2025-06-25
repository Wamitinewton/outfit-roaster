package com.spring.outfit_rater.repository;

import com.spring.outfit_rater.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    
    @Query("SELECT p FROM RoomParticipant p WHERE p.room.roomCode = :roomCode AND p.userId = :userId")
    Optional<RoomParticipant> findByRoomCodeAndUserId(@Param("roomCode") String roomCode, @Param("userId") String userId);
    
    @Query("SELECT p FROM RoomParticipant p WHERE p.room.roomCode = :roomCode AND p.isActive = true ORDER BY p.joinedAt ASC")
    List<RoomParticipant> findActiveParticipantsByRoomCode(@Param("roomCode") String roomCode);
    
    @Query("SELECT p FROM RoomParticipant p WHERE p.userId = :userId AND p.isActive = true")
    List<RoomParticipant> findActiveParticipantsByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(p) FROM RoomParticipant p WHERE p.room.roomCode = :roomCode AND p.isActive = true")
    int countActiveParticipantsByRoomCode(@Param("roomCode") String roomCode);
    
    @Modifying
    @Query("UPDATE RoomParticipant p SET p.lastSeenAt = :lastSeen WHERE p.userId = :userId AND p.room.roomCode = :roomCode")
    void updateLastSeenAt(@Param("userId") String userId, @Param("roomCode") String roomCode, @Param("lastSeen") LocalDateTime lastSeen);
    
    @Modifying
    @Query("UPDATE RoomParticipant p SET p.isActive = false WHERE p.userId = :userId AND p.room.roomCode = :roomCode")
    void deactivateParticipant(@Param("userId") String userId, @Param("roomCode") String roomCode);
    
    @Query("SELECT p FROM RoomParticipant p WHERE p.lastSeenAt < :threshold AND p.isActive = true")
    List<RoomParticipant> findInactiveParticipants(@Param("threshold") LocalDateTime threshold);
}

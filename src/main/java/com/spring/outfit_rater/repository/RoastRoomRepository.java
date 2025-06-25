package com.spring.outfit_rater.repository;

import com.spring.outfit_rater.model.RoastRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoastRoomRepository extends JpaRepository<RoastRoom, Long> {
    
    @Query("SELECT r FROM RoastRoom r WHERE r.roomCode = :roomCode")
    Optional<RoastRoom> findByRoomCode(@Param("roomCode") String roomCode);
    
    @Query("SELECT r FROM RoastRoom r WHERE r.creatorId = :creatorId ORDER BY r.createdAt DESC")
    List<RoastRoom> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") String creatorId);
    
    @Query("SELECT r FROM RoastRoom r WHERE r.isActive = true AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<RoastRoom> findActiveRooms(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM RoastRoom r WHERE r.isActive = true AND r.isPrivate = false AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<RoastRoom> findPublicActiveRooms(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM RoastRoom r WHERE r.expiresAt <= :now AND r.isActive = true")
    List<RoastRoom> findExpiredRooms(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(r) FROM RoastRoom r WHERE r.creatorId = :creatorId AND r.isActive = true AND r.expiresAt > :now")
    int countActiveRoomsByCreator(@Param("creatorId") String creatorId, @Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM RoastRoom r JOIN r.participants p WHERE p.userId = :userId AND p.isActive = true AND r.isActive = true ORDER BY p.lastSeenAt DESC")
    List<RoastRoom> findRoomsByParticipant(@Param("userId") String userId);
    
    boolean existsByRoomCode(String roomCode);
}

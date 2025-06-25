package com.spring.outfit_rater.repository;

import com.spring.outfit_rater.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomCode IS NULL ORDER BY cm.createdAt DESC LIMIT 50")
    List<ChatMessage> findRecentGlobalMessages();
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomCode = :roomCode ORDER BY cm.createdAt DESC LIMIT :limit")
    List<ChatMessage> findRecentRoomMessages(@Param("roomCode") String roomCode, @Param("limit") int limit);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId AND cm.roomCode IS NULL ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId AND cm.roomCode = :roomCode ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserIdAndRoomCodeOrderByCreatedAtDesc(@Param("userId") String userId, @Param("roomCode") String roomCode);
    
    @Query("SELECT cm FROM ChatMessage cm ORDER BY cm.createdAt DESC LIMIT 100")
    List<ChatMessage> findTop100ByOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomCode = :roomCode")
    long countMessagesByRoomCode(@Param("roomCode") String roomCode);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomCode = :roomCode AND cm.type = :type ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByRoomCodeAndType(@Param("roomCode") String roomCode, @Param("type") ChatMessage.MessageType type);

}
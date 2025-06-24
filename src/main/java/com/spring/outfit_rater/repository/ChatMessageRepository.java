package com.spring.outfit_rater.repository;

import com.spring.outfit_rater.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT cm FROM ChatMessage cm ORDER BY cm.createdAt DESC LIMIT 50")
    List<ChatMessage> findTop50ByOrderByCreatedAtDesc();
}

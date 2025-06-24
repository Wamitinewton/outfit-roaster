package com.spring.outfit_rater.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.spring.outfit_rater.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findTop50ByOrderByCreatedAtDesc();
    
    @Query("SELECT c FROM ChatMessage c WHERE c.createdAt >= :since ORDER BY c.createdAt ASC")
    List<ChatMessage> findRecentMessages(LocalDateTime since);
    
    List<ChatMessage> findByUserIpOrderByCreatedAtDesc(String userIp);
}
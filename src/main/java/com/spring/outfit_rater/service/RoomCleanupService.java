package com.spring.outfit_rater.service;

import com.spring.outfit_rater.model.RoomParticipant;
import com.spring.outfit_rater.repository.RoomParticipantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class RoomCleanupService {

    private final RoomService roomService;
    private final RoomParticipantRepository participantRepository;
    
    private static final int INACTIVE_THRESHOLD_MINUTES = 30;

    public RoomCleanupService(RoomService roomService, RoomParticipantRepository participantRepository) {
        this.roomService = roomService;
        this.participantRepository = participantRepository;
    }


    @Scheduled(fixedRate = 30 * 60 * 1000) 
    @Transactional
    public void cleanupExpiredRoomsAndInactiveParticipants() {
        log.info("Starting scheduled cleanup of rooms and participants");
        
        try {
            roomService.deactivateExpiredRooms();
            
            cleanupInactiveParticipants();
            
            log.info("Completed scheduled cleanup successfully");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }

   
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void updateParticipantActivityStatus() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_THRESHOLD_MINUTES);
            List<RoomParticipant> inactiveParticipants = participantRepository.findInactiveParticipants(threshold);
            
            for (RoomParticipant participant : inactiveParticipants) {
                if (participant.getIsActive()) {
                    participant.setIsActive(false);
                    participantRepository.save(participant);
                    
                    log.debug("Marked participant {} as inactive in room {}", 
                             participant.getUserId(), 
                             participant.getRoom().getRoomCode());
                }
            }
            
            if (!inactiveParticipants.isEmpty()) {
                log.info("Updated activity status for {} participants", inactiveParticipants.size());
            }
        } catch (Exception e) {
            log.warn("Error updating participant activity status", e);
        }
    }

    private void cleanupInactiveParticipants() {
        try {
            LocalDateTime inactiveThreshold = LocalDateTime.now().minusHours(2);
            List<RoomParticipant> longInactiveParticipants = participantRepository.findInactiveParticipants(inactiveThreshold);
            
            int removedCount = 0;
            for (RoomParticipant participant : longInactiveParticipants) {
                if (!participant.getIsActive()) {
                    if (participant.getRole() != RoomParticipant.ParticipantRole.CREATOR) {
                        participantRepository.delete(participant);
                        removedCount++;
                        
                        log.debug("Removed inactive participant {} from room {}", 
                                 participant.getUserId(), 
                                 participant.getRoom().getRoomCode());
                    }
                }
            }
            
            if (removedCount > 0) {
                log.info("Removed {} inactive participants from rooms", removedCount);
            }
        } catch (Exception e) {
            log.error("Error cleaning up inactive participants", e);
        }
    }

    @Transactional
    public void performManualCleanup() {
        log.info("Performing manual cleanup");
        
        roomService.deactivateExpiredRooms();
        cleanupInactiveParticipants();
        updateParticipantActivityStatus();
        
        log.info("Manual cleanup completed");
    }
}
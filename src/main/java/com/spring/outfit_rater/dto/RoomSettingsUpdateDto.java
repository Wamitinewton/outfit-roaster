package com.spring.outfit_rater.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSettingsUpdateDto {
    
    @Size(min = 3, max = 100, message = "Room name must be between 3 and 100 characters")
    private String roomName;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Min(value = 2, message = "Room must allow at least 2 participants")
    @Max(value = 50, message = "Room cannot exceed 50 participants")
    private Integer maxParticipants;
    
    @Min(value = 1, message = "Extension must be at least 1 hour")
    @Max(value = 72, message = "Extension cannot exceed 72 hours")
    private Integer extendHours;
    
    private Boolean isActive;
}

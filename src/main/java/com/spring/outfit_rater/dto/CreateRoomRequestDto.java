package com.spring.outfit_rater.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequestDto {
    
    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 100, message = "Room name must be between 3 and 100 characters")
    private String roomName;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Min(value = 2, message = "Room must allow at least 2 participants")
    @Max(value = 50, message = "Room cannot exceed 50 participants")
    @Builder.Default
    private Integer maxParticipants = 20;
    
    @Builder.Default
    private Boolean isPrivate = true;
    
    @Min(value = 1, message = "Room duration must be at least 1 hour")
    @Max(value = 72, message = "Room duration cannot exceed 72 hours")
    @Builder.Default
    private Integer durationHours = 24;
}




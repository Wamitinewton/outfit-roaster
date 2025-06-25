package com.spring.outfit_rater.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequestDto {
    
    @NotBlank(message = "Room code is required")
    @Size(min = 8, max = 8, message = "Room code must be exactly 8 characters")
    private String roomCode;
    
    @Size(max = 50, message = "Display name cannot exceed 50 characters")
    private String displayName;
}

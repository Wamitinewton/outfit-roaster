package com.spring.outfit_rater.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDto {
    
    private Boolean success;
    private String message;
    private RoomDto room;
    private String errorCode;
    
    public static RoomResponseDto success(RoomDto room, String message) {
        return RoomResponseDto.builder()
                .success(true)
                .message(message)
                .room(room)
                .build();
    }
    
    public static RoomResponseDto error(String message, String errorCode) {
        return RoomResponseDto.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}

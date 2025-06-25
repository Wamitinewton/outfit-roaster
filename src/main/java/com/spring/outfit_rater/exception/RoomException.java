package com.spring.outfit_rater.exception;

import lombok.Getter;

@Getter
public class RoomException extends RuntimeException {
    
    private final String errorCode;
    
    public RoomException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RoomException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_EXPIRED = "ROOM_EXPIRED";
    public static final String ROOM_FULL = "ROOM_FULL";
    public static final String ROOM_INACTIVE = "ROOM_INACTIVE";
    public static final String ALREADY_JOINED = "ALREADY_JOINED";
    public static final String NOT_PARTICIPANT = "NOT_PARTICIPANT";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ROOM_LIMIT_EXCEEDED = "ROOM_LIMIT_EXCEEDED";
    public static final String CODE_GENERATION_FAILED = "CODE_GENERATION_FAILED";
    public static final String INVALID_PARTICIPANT_LIMIT = "INVALID_PARTICIPANT_LIMIT";
}

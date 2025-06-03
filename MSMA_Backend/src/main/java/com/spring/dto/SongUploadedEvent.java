package com.spring.dto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SongUploadedEvent extends ApplicationEvent {
    private final Long songId;

    public SongUploadedEvent(Object source, Long songId) {
        super(source);
        this.songId = songId;
    }
}


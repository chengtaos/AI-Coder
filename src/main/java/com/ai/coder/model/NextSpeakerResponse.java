package com.ai.coder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 下一步发言者响应
 */
@Data
public class NextSpeakerResponse {
    @JsonProperty("next_speaker")
    private String nextSpeaker;

    @JsonProperty("reasoning")
    private String reasoning;

    public NextSpeakerResponse() {
    }

    public NextSpeakerResponse(String nextSpeaker, String reasoning) {
        this.nextSpeaker = nextSpeaker;
        this.reasoning = reasoning;
    }

    public boolean isModelNext() {
        return "model".equals(nextSpeaker);
    }

    public boolean isUserNext() {
        return "user".equals(nextSpeaker);
    }

    @Override
    public String toString() {
        return String.format("NextSpeaker{speaker='%s', reasoning='%s'}", nextSpeaker, reasoning);
    }
}
package com.chartflow.core.model.dto.chart;

import lombok.Data;

import java.util.List;

@Data
public class OllamaChatRequest {
    private String model;
    private List<Message> messages;
    private boolean stream;

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
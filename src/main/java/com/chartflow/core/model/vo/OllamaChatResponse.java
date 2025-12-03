package com.chartflow.core.model.vo;


import lombok.Data;

import java.util.List;

@Data
public class OllamaChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
        private String finish_reason;
        private Integer index;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
package com.example.demo5.service;

import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class QuestionGenerationService {

    private final OpenAiService openAiService;

    public QuestionGenerationService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String generateQuestionFromTopic(String topic) {
        String customPrompt = """
            당신은 주어진 주제를 자연스럽고 친근한 한국어 질문으로 바꾸는 AI입니다.
            질문은 반드시 물음표(?)로 끝나거나, 그에 준하는 의문형으로 마무리되어야 합니다.
            예시:
            - 주제: 최근에 본 영화
            - 질문: 최근에 보신 영화 중에 가장 인상 깊었던 작품이 있으신가요?
            - 주제: 주말 계획
            - 질문: 혹시 이번 주말에 특별한 계획이라도 있으세요?
            """;
        // OpenAiService를 호출하여 AI 응답을 받습니다.
        return openAiService.getChatResponse(Collections.singletonList(new com.example.demo5.dto.ChatMessage("User", topic)), customPrompt);
    }
}

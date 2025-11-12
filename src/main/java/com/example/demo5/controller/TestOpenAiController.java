package com.example.demo5.controller;

import com.example.demo5.service.OpenAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestOpenAiController {

    private final OpenAiService openAiService;

    public TestOpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    /**
     * OpenAI API 테스트를 위한 엔드포인트입니다.
     * JSON 형식으로 {"question": "당신의 질문"} 과 같이 요청을 보내면,
     * AI의 답변이 서버 콘솔에 출력됩니다.
     */
    @PostMapping("/api/test/openai")
    public ResponseEntity<String> testOpenAi(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body("'question' 필드가 필요합니다.");
        }

        System.out.println("========================================");
        System.out.println("사용자 질문: " + question);

        String aiResponse = openAiService.getChatResponse(question);

        System.out.println("AI 답변: " + aiResponse);
        System.out.println("========================================");

        return ResponseEntity.ok("AI 답변: " + aiResponse);
    }

    @PostMapping("/")
    public String test() {
        return "안녕";
    }
}

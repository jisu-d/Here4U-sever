package com.example.demo5.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    private final ChatModel chatModel;

    // Spring AI가 application.properties에 설정된 API 키를 사용하여
    // ChatModel Bean을 자동으로 생성하고 주입해줍니다.
    public OpenAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 주어진 질문을 OpenAI API로 보내고 답변을 받아옵니다.
     * @param userQuestion 사용자의 질문
     * @return OpenAI 모델의 답변
     */
    public String getChatResponse(String userQuestion) {
        try {
            // Spring AI의 ChatClient를 사용하여 API 호출
            return chatModel.call(userQuestion);
        } catch (Exception e) {
            System.err.println("OpenAI API 호출 중 오류 발생: " + e.getMessage());
            return "AI 응답을 가져오는 데 실패했습니다.";
        }
    }
}
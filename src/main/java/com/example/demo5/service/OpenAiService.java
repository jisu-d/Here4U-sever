package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenAiService {

    private final ChatModel chatModel;
    private static final String SYSTEM_PROMPT = """
            당신은 사용자의 이야기를 들어주고 공감하며, 가끔은 조언을 해주는 AI 상담가 입니다.
            대화의 전체 맥락을 파악하고, 사용자와 더 깊은 대화를 할 수 있도록 유도한다.
            너무 말은 딱딱하게 하지 말고 부드럽게 답변해줘.
            답변은 항상 한국어로, 두문장에서 세문장 정도로 대답하며 상황에 따라 공감하고 조언을 할 수도 있고 그에 대한 질문도 던질 수 있어.
            """;

    public OpenAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 간단한 문자열 질문을 받아 AI의 답변을 생성합니다.
     * 테스트 컨트롤러와의 호환성을 위해 존재합니다.
     * @param userQuestion 사용자의 간단한 질문
     * @return AI가 생성한 답변
     */
    public String getChatResponse(String userQuestion) {
        return getChatResponse(List.of(new ChatMessage("User", userQuestion)), null); // Call new method with null systemPrompt
    }

    /**
     * 시스템 프롬프트와 전체 대화 기록을 바탕으로 AI의 다음 응답을 생성합니다.
     * @param history 현재까지의 대화 기록
     * @return AI가 생성한 다음 질문
     */
    public String getChatResponse(List<ChatMessage> history) {
        return getChatResponse(history, null); // Call new method with null systemPrompt
    }

    /**
     * 주어진 시스템 프롬프트와 전체 대화 기록을 바탕으로 AI의 다음 응답을 생성합니다.
     * @param history 현재까지의 대화 기록
     * @param customSystemPrompt 사용할 시스템 프롬프트 (null이면 기본 SYSTEM_PROMPT 사용)
     * @return AI가 생성한 다음 질문
     */
    public String getChatResponse(List<ChatMessage> history, String customSystemPrompt) {
        try {
            // 1. 시스템 메시지 생성 (customSystemPrompt가 있으면 사용, 없으면 기본 SYSTEM_PROMPT 사용)
            SystemMessage systemMessage;
            if (StringUtils.hasText(customSystemPrompt)) {
                systemMessage = new SystemMessage(customSystemPrompt);
            } else {
                systemMessage = new SystemMessage(SYSTEM_PROMPT);
            }

            // 2. 이전 대화 기록을 Spring AI의 Message 객체로 변환
            List<Message> conversationMessages = history.stream()
                    .map(chatMessage -> {
                        if ("AI".equalsIgnoreCase(chatMessage.speaker())) {
                            return new AssistantMessage(chatMessage.message());
                        } else {
                            return new UserMessage(chatMessage.message());
                        }
                    })
                    .collect(Collectors.toList());

            // 3. 시스템 메시지와 대화 기록을 합쳐 최종 메시지 리스트 생성
            List<Message> finalMessages = new ArrayList<>();
            finalMessages.add(systemMessage);
            finalMessages.addAll(conversationMessages);

            // 4. Prompt 객체를 생성하여 API 호출
            Prompt prompt = new Prompt(finalMessages);

            // 5. 해당 버전의 API 제약으로 인해, getOutput()의 getContent() 메소드로 내용을 가져옵니다.
            return chatModel.call(prompt).getResult().getOutput().getText();

        } catch (Exception e) {
            System.err.println("OpenAI API 호출 중 오류 발생: " + e.getMessage());
            // 비상 시를 대비한 기본 응답
            return "죄송합니다. 시스템에 오류가 발생하여 답변을 드릴 수 없습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}
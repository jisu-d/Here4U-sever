package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QnaService {

    private static final int MAX_TURNS = 10;
    private static final String HANGUP_KEYWORD = "종료";
    private static final String FINAL_MESSAGE = "오늘 함께 이야기 나눌 수 있어서 의미 있는 시간이었습니다. 편안한 하루 보내시고, 다음에 또 뵙겠습니다.";
    private static final String TIMEOUT_MESSAGE = "응답이 없어 통화를 종료합니다.";
    private static final String HANGUP_MESSAGE = "요청에 따라 통화를 종료합니다.";

    private final TwilioService twilioService;
    private final OpenAiService openAiService;

    // 데이터베이스 대신 인-메모리 맵을 사용하여 통화별 대화 내용 저장
    private final Map<String, List<ChatMessage>> conversationStorage = new ConcurrentHashMap<>();

    public QnaService(TwilioService twilioService, OpenAiService openAiService) {
        this.twilioService = twilioService;
        this.openAiService = openAiService;
    }

    /**
     * AI 상담을 시작합니다.
     * OpenAI를 호출하여 첫 번째 질문을 받고 TwiML을 생성합니다.
     */
    public String startSurvey(String callSid, String baseUrl) {
        // 하드코딩된 첫 인사말로 변경
        String firstQuestion = "안녕하세요, AI 상담가입니다. 오늘 어떤 이야기를 나누고 싶으신가요?";
        System.out.println("AI 질문 (1): " + firstQuestion);

        // 대화 기록 초기화 및 AI의 첫 질문 저장
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage("AI", firstQuestion));
        conversationStorage.put(callSid, history);

        return twilioService.createGatherTwiML(firstQuestion, baseUrl);
    }

    /**
     * 사용자의 답변을 처리하고 AI의 다음 질문을 받거나 통화를 종료합니다.
     */
    public String processSurveyResponse(String callSid, String speechResult, String baseUrl) {
        // 1. 타임아웃 처리
        if (!StringUtils.hasText(speechResult)) {
            System.out.println("응답 시간 초과로 통화를 종료합니다.");
            printAndClearHistory(callSid);
            return twilioService.createHangupTwiML(TIMEOUT_MESSAGE);
        }

        System.out.println("사용자 답변: " + speechResult);

        // 2. 사용자의 종료 요청 처리
        if (speechResult.contains(HANGUP_KEYWORD)) {
            System.out.println("사용자의 요청으로 통화를 종료합니다.");
            printAndClearHistory(callSid);
            return twilioService.createHangupTwiML(HANGUP_MESSAGE);
        }

        // 3. 정상 답변 처리
        List<ChatMessage> history = conversationStorage.getOrDefault(callSid, new ArrayList<>());
        history.add(new ChatMessage("User", speechResult));

        // 4. 대화 턴(turn) 수 계산 (사용자 답변 기준)
        long userTurns = history.stream().filter(m -> "User".equalsIgnoreCase(m.speaker())).count();

        if (userTurns < MAX_TURNS) {
            // 5. 다음 질문 생성 (10턴 미만)
            String nextQuestion = openAiService.getChatResponse(history);
            System.out.println("AI 질문 (" + (userTurns + 1) + "): " + nextQuestion);
            history.add(new ChatMessage("AI", nextQuestion));
            conversationStorage.put(callSid, history); // 맵에 다시 저장
            return twilioService.createGatherTwiML(nextQuestion, baseUrl);
        } else {
            // 6. 마지막 인사 및 통화 종료 (10턴 도달)
            System.out.println("10턴 대화가 완료되어 통화를 종료합니다.");
            printAndClearHistory(callSid);
            return twilioService.createHangupTwiML(FINAL_MESSAGE);
        }
    }

    /**
     * 통화가 종료될 때 대화 기록을 출력하고 저장소에서 삭제합니다.
     * @param callSid 통화 식별자
     */
    private void printAndClearHistory(String callSid) {
        List<ChatMessage> history = conversationStorage.get(callSid);
        if (history != null && !history.isEmpty()) {
            System.out.println("\n--- 통화 종료: 대화 기록 (" + callSid + ") ---");
            history.forEach(message ->
                    System.out.println(message.speaker() + ": " + message.message())
            );
            System.out.println("--- 기록 종료 ---\n");
        }
        conversationStorage.remove(callSid);
    }
}
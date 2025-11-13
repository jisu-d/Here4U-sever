package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import com.example.demo5.entity.CallLog;
import com.example.demo5.repository.CallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QnaService {

    private static final int MAX_TURNS = 10;
    private static final String HANGUP_KEYWORD = "종료";
    private static final String FINAL_MESSAGE = "오늘 함께 이야기 나눌 수 있어서 의미 있는 시간이었습니다. 편안한 하루 보내시고, 다음에 또 뵙겠습니다.";
    private static final String TIMEOUT_MESSAGE = "응답이 없어 통화를 종료합니다.";
    private static final String HANGUP_MESSAGE = "요청에 따라 통화를 종료합니다.";

    private final TwilioService twilioService;
    private final OpenAiService openAiService;
    private final CallLogRepository callLogRepository;
    private final ObjectMapper objectMapper;


    // 데이터베이스 대신 인-메모리 맵을 사용하여 통화별 대화 내용 저장
    private final Map<String, List<ChatMessage>> conversationStorage = new ConcurrentHashMap<>();

    public QnaService(TwilioService twilioService, OpenAiService openAiService, CallLogRepository callLogRepository, ObjectMapper objectMapper) {
        this.twilioService = twilioService;
        this.openAiService = openAiService;
        this.callLogRepository = callLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * AI 상담을 시작합니다.
     * OpenAI를 호출하여 첫 번째 질문을 받고 TwiML을 생성합니다.
     */
    public String startSurvey(String callSid, String baseUrl) {
        // 하드코딩된 첫 인사말로 변경
        String firstQuestion = "안녕하세요, AI 상담가입니다. 오늘 어떤 이야기를 나누고 싶으신가요?";
        log.info("AI First Question (CallSid: {}): {}", callSid, firstQuestion);

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
            log.info("Call timed out (CallSid: {}).");
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.FAILED);
            return twilioService.createHangupTwiML(TIMEOUT_MESSAGE);
        }

        log.info("User Response (CallSid: {}): {}", callSid, speechResult);

        // 2. 사용자의 종료 요청 처리
        if (speechResult.contains(HANGUP_KEYWORD)) {
            log.info("User requested to end the call (CallSid: {}).");
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.COMPLETED);
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
            log.info("AI Question #{} (CallSid: {}): {}", userTurns + 1, callSid, nextQuestion);
            history.add(new ChatMessage("AI", nextQuestion));
            conversationStorage.put(callSid, history); // 맵에 다시 저장
            return twilioService.createGatherTwiML(nextQuestion, baseUrl);
        } else {
            // 6. 마지막 인사 및 통화 종료 (10턴 도달)
            log.info("Max turns reached. Ending call (CallSid: {}).");
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.COMPLETED);
            return twilioService.createHangupTwiML(FINAL_MESSAGE);
        }
    }

    /**
     * 통화가 종료될 때 대화 기록을 DB에 저장하고 저장소에서 삭제합니다.
     * @param callSid 통화 식별자
     * @param finalStatus 통화의 최종 상태
     */
    @Transactional
    public void finalizeAndSaveCallLog(String callSid, CallLog.CallStatus finalStatus) {
        List<ChatMessage> history = conversationStorage.get(callSid);
        if (history == null) {
            log.warn("No conversation history found for CallSid: {}", callSid);
            return;
        }

        callLogRepository.findByCallSid(callSid).ifPresentOrElse(callLog -> {
            try {
                // 대화 내용을 JSON으로 변환
                String callDataJson = objectMapper.writeValueAsString(history);
                callLog.setCallData(callDataJson);
                callLog.setStatus(finalStatus);
                callLogRepository.save(callLog);
                log.info("Successfully saved call log for CallSid: {}", callSid);

            } catch (JsonProcessingException e) {
                log.error("Failed to serialize call data for CallSid: {}", callSid, e);
                // JSON 변환 실패 시에도 상태는 업데이트
                callLog.setStatus(CallLog.CallStatus.FAILED);
                callLog.setCallData("{\"error\": \"Failed to process conversation data.\"}");
                callLogRepository.save(callLog);
            }
        }, () -> {
            log.error("Could not find CallLog entry for CallSid: {}", callSid);
        });

        // 메모리에서 대화 내용 삭제
        conversationStorage.remove(callSid);
    }
}
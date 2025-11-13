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
    private final KeywordAnalysisService keywordAnalysisService;
    private final MemberStatusAnalysisService memberStatusAnalysisService; // New injection


    // 데이터베이스 대신 인-메모리 맵을 사용하여 통화별 대화 내용 저장
    private final Map<String, List<ChatMessage>> conversationStorage = new ConcurrentHashMap<>();

    public QnaService(TwilioService twilioService, OpenAiService openAiService, CallLogRepository callLogRepository, ObjectMapper objectMapper, KeywordAnalysisService keywordAnalysisService, MemberStatusAnalysisService memberStatusAnalysisService) {
        this.twilioService = twilioService;
        this.openAiService = openAiService;
        this.callLogRepository = callLogRepository;
        this.objectMapper = objectMapper;
        this.keywordAnalysisService = keywordAnalysisService;
        this.memberStatusAnalysisService = memberStatusAnalysisService; // Assign new service
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
            log.info("Call timed out (CallSid: {}).", callSid);
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.FAILED, "응답 시간 초과");
            return twilioService.createHangupTwiML(TIMEOUT_MESSAGE);
        }

        log.info("User Response (CallSid: {}): {}", callSid, speechResult);
        List<ChatMessage> history = conversationStorage.getOrDefault(callSid, new ArrayList<>());
        history.add(new ChatMessage("User", speechResult));

        // 2. 음성 사서함 감지 (첫 응답인 경우)
        long userTurns = history.stream().filter(m -> "User".equalsIgnoreCase(m.speaker())).count();
        if (userTurns == 1) {
            if (speechResult.contains("음성사서함") || speechResult.contains("소리샘") || speechResult.contains("남겨주세요")) {
                log.info("Voicemail detected. Ending call (CallSid: {}).", callSid);
                finalizeAndSaveCallLog(callSid, CallLog.CallStatus.FAILED, "음성 사서함 감지");
                return twilioService.createHangupTwiML("음성 사서함이 감지되어 통화를 종료합니다.");
            }
        }

        // 3. 사용자의 종료 요청 처리
        if (speechResult.contains(HANGUP_KEYWORD)) {
            log.info("User requested to end the call (CallSid: {}).", callSid);
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.COMPLETED, "사용자 요청");
            return twilioService.createHangupTwiML(HANGUP_MESSAGE);
        }

        // 4. 정상 답변 처리
        if (userTurns < MAX_TURNS) {
            // 5. 다음 질문 생성 (10턴 미만)
            String nextQuestion = openAiService.getChatResponse(history);
            log.info("AI Question #{} (CallSid: {}): {}", userTurns + 1, callSid, nextQuestion);
            history.add(new ChatMessage("AI", nextQuestion));
            conversationStorage.put(callSid, history); // 맵에 다시 저장
            return twilioService.createGatherTwiML(nextQuestion, baseUrl);
        } else {
            // 6. 마지막 인사 및 통화 종료 (10턴 도달)
            log.info("Max turns reached. Ending call (CallSid: {}).", callSid);
            finalizeAndSaveCallLog(callSid, CallLog.CallStatus.COMPLETED, "최대 대화 도달");
            return twilioService.createHangupTwiML(FINAL_MESSAGE);
        }
    }

    /**
     * 통화가 종료될 때 대화 기록을 DB에 저장하고 저장소에서 삭제합니다.
     * @param callSid 통화 식별자
     * @param finalStatus 통화의 최종 상태
     * @param reason 종료 사유
     */
    @Transactional
    public void finalizeAndSaveCallLog(String callSid, CallLog.CallStatus finalStatus, String reason) {
        List<ChatMessage> history = conversationStorage.get(callSid);
        // 대화 기록이 없는 경우(예: 음성사서함 감지 직후)를 위해 null 체크 후 빈 리스트 할당
        if (history == null) {
            history = new ArrayList<>();
        }
        // 종료 사유를 대화 기록에 추가
        history.add(new ChatMessage("System", "Call ended. Reason: " + reason));

        final List<ChatMessage> effectivelyFinalHistory = history;

        callLogRepository.findByCallSid(callSid).ifPresentOrElse(callLog -> {
            try {
                // 대화 내용을 JSON으로 변환
                String callDataJson = objectMapper.writeValueAsString(effectivelyFinalHistory);
                callLog.setCallData(callDataJson);
                callLog.setStatus(finalStatus);
                callLogRepository.save(callLog);
                log.info("Successfully saved call log for CallSid: {}", callSid);

                // Trigger keyword analysis
                keywordAnalysisService.analyzeAndSaveKeywords(callLog.getMember().getMemberId(), callLog.getRequestedAt());

                // Trigger member status analysis
                memberStatusAnalysisService.analyzeAndSaveMemberStatus(callLog.getMember().getMemberId(), callLog.getRequestedAt());

            } catch (JsonProcessingException e) {
                log.error("Failed to serialize call data for CallSid: {}", callSid, e);
                callLog.setStatus(CallLog.CallStatus.FAILED);
                // 에러 메시지를 Map으로 만든 후 다시 JSON으로 변환하여 저장
                Map<String, String> errorData = Map.of(
                        "오류", "대화 내용 JSON 변환 실패",
                        "사유", e.getMessage()
                );
                try {
                    callLog.setCallData(objectMapper.writeValueAsString(errorData));
                } catch (JsonProcessingException ex) {
                    // 이중 실패 시, 간단한 텍스트로 저장
                    callLog.setCallData("{\"error\": \"Failed to process conversation data and failed to serialize error message.\"}");
                }
                callLogRepository.save(callLog);
            }
        }, () -> {
            log.error("Could not find CallLog entry for CallSid: {}", callSid);
        });

        // 메모리에서 대화 내용 삭제
        conversationStorage.remove(callSid);
    }

    /**
     * Twilio Status Callback을 처리하여 예기치 않은 통화 종료를 처리합니다.
     * @param callSid 통화 식별자
     * @param callStatus Twilio가 보낸 통화 상태
     */
    @Transactional
    public void handleCallTermination(String callSid, String callStatus) {
        log.info("Received status callback for CallSid: {}. Status: {}", callSid, callStatus);

        callLogRepository.findByCallSid(callSid).ifPresent(callLog -> {
            // 이미 callData가 저장되었다면(정상 종료된 경우), 아무것도 하지 않음
            if (StringUtils.hasText(callLog.getCallData())) {
                log.info("Call log for {} already finalized. Ignoring status callback.", callSid);
                // 최종 상태 업데이트가 필요한 경우를 위해 메모리만 정리하고 종료
                conversationStorage.remove(callSid);
                return;
            }

            // Twilio 상태를 내부 상태로 매핑
            CallLog.CallStatus finalStatus = switch (callStatus) {
                case "completed" -> CallLog.CallStatus.COMPLETED;
                case "canceled", "failed", "no-answer" -> CallLog.CallStatus.FAILED;
                default -> CallLog.CallStatus.FAILED;
            };

            log.warn("Call {} terminated unexpectedly with status {}. Saving conversation log.", callSid, callStatus);
            finalizeAndSaveCallLog(callSid, finalStatus, "Unexpected termination: " + callStatus);
        });
    }
}
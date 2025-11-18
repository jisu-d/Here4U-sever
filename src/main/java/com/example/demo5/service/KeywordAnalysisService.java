package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import com.example.demo5.dto.analysis.AnalysisResponse;
import com.example.demo5.entity.CallLog;
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.MemberKeywordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordAnalysisService {

    private final OpenAiService openAiService;
    private final CallLogRepository callLogRepository;
    private final MemberKeywordRepository memberKeywordRepository; // DB 저장을 위해 다시 추가
    private final ObjectMapper objectMapper;

    private static final String ANALYSIS_SYSTEM_PROMPT = """
            "너는 대화 내용을 분석하고 세 가지 항목을 추출하는 전문가야.
            사용자의 대화 내용이 주어지면 다음 규칙을 반드시 지켜서 결과를 반환해줘.

            1. 주요 키워드들을 공백으로 구분하여 한 줄로 요약.
            2. 대화의 전반적인 분위기나 감정을 '긍정적', '부정적', '중립' 등 한 단어로 표현.
            3. 분석 내용을 바탕으로 사용자에게 전달할 격려나 조언의 피드백을 한 문장으로 작성.

            결과는 반드시 ['키워드 목록', '감정', '피드백 문장'] 형태의 파싱 가능한 단일 리스트 문자열로만 반환해야 해. 다른 부가적인 설명은 절대 추가하지 마."
            """;

    /**
     * 대화를 분석하고 그 결과를 DB에 저장합니다. (QnaService에서 호출)
     */
    @Transactional
    public void analyzeAndSaveConversationAnalysis(String memberId) {
        // 1. AI를 통해 대화 내용 분석
        AnalysisResponse analysisResponse = performAnalysis(memberId);

        // 2. 분석 결과를 JSON 문자열로 변환
        try {
            String analysisJson = objectMapper.writeValueAsString(analysisResponse);

            // 3. DB에 저장
            memberKeywordRepository.findByMember_MemberId(memberId).ifPresentOrElse(memberKeyword -> {
                memberKeyword.setKeyword(analysisJson);
                memberKeywordRepository.save(memberKeyword);
                log.info("회원 ID {}의 대화 분석 결과를 DB에 저장했습니다.", memberId);
            }, () -> {
                log.error("회원 ID {}에 해당하는 MemberKeyword 엔티티를 찾을 수 없습니다.", memberId);
                throw new EntityNotFoundException("MemberKeyword not found for memberId: " + memberId);
            });

        } catch (JsonProcessingException e) {
            log.error("AnalysisResponse를 JSON으로 변환하는 데 실패했습니다. memberId: {}", memberId, e);
        }
    }

    /**
     * AI를 통해 실제 분석을 수행하는 내부 로직
     */
    private AnalysisResponse performAnalysis(String memberId) {
        LocalDateTime analysisEndTime = LocalDateTime.now();
        LocalDateTime analysisStartTime = analysisEndTime.minusDays(7);

        List<CallLog> recentCallLogs = callLogRepository.findByMember_MemberIdAndRequestedAtBetween(memberId, analysisStartTime, analysisEndTime);

        if (recentCallLogs.isEmpty()) {
            log.info("분석할 최근 통화 기록이 없습니다. memberId: {}", memberId);
            return new AnalysisResponse("통화 기록 없음", "정보 없음", "최근 통화 기록이 없어 분석할 수 없습니다.");
        }

        String aggregatedConversation = recentCallLogs.stream()
                .map(this::extractUserMessagesFromCallLog)
                .collect(Collectors.joining("\n"));

        if (aggregatedConversation.trim().isEmpty()) {
            log.info("통화 기록에서 유효한 대화 내용을 찾을 수 없습니다. memberId: {}", memberId);
            return new AnalysisResponse("유효 대화 없음", "정보 없음", "최근 통화에서 유효한 대화 내용이 없어 분석할 수 없습니다.");
        }

        List<ChatMessage> messages = List.of(new ChatMessage("User", "대화:\n" + aggregatedConversation));
        log.info("AI 분석을 위해 OpenAI로 데이터를 전송합니다. MemberId: {}", memberId);
        String aiResponse = openAiService.getChatResponse(messages, ANALYSIS_SYSTEM_PROMPT);

        return parseAiResponse(aiResponse);
    }

    private String extractUserMessagesFromCallLog(CallLog callLog) {
        if (callLog.getCallData() == null || callLog.getCallData().isEmpty()) {
            return "";
        }
        try {
            List<ChatMessage> chatHistory = objectMapper.readValue(callLog.getCallData(), new TypeReference<>() {});
            return chatHistory.stream()
                    .filter(m -> "User".equalsIgnoreCase(m.speaker()))
                    .map(ChatMessage::message)
                    .collect(Collectors.joining("\n"));
        } catch (JsonProcessingException e) {
            log.error("통화 기록 파싱 중 오류 발생 callLogId: {}", callLog.getCallLogId(), e);
            return "";
        }
    }

    private AnalysisResponse parseAiResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.error("AI로부터 비어있는 응답을 받았습니다.");
            return new AnalysisResponse("분석 실패", "오류", "AI 응답이 없습니다.");
        }

        // 파싱 로직: ['키워드', '감정', '피드백']
        String content = aiResponse.trim();
        if (content.startsWith("['") && content.endsWith("']")) {
            content = content.substring(2, content.length() - 2);
            String[] parts = content.split("', '", 3);

            if (parts.length == 3) {
                return new AnalysisResponse(parts[0], parts[1], parts[2]);
            }
        }

        log.error("AI 응답을 파싱할 수 없습니다. 응답: {}", aiResponse);
        return new AnalysisResponse("분석 실패", "오류", "AI 응답 형식에 문제가 있습니다.");
    }
}

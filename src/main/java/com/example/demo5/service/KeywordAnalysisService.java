package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import com.example.demo5.entity.CallLog;
import com.example.demo5.entity.MemberKeyword;
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.MemberKeywordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordAnalysisService {

    private final KeywordAiService keywordAiService;
    private final MemberKeywordRepository memberKeywordRepository;
    private final CallLogRepository callLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void analyzeAndSaveKeywords(String memberId, LocalDateTime analysisEndTime) {
        LocalDateTime analysisStartTime = analysisEndTime.minusDays(7);

        // 1. 지난 7일간의 통화 기록 가져오기
        List<CallLog> recentCallLogs = callLogRepository.findByMember_MemberIdAndRequestedAtBetween(memberId, analysisStartTime, analysisEndTime);

        if (recentCallLogs.isEmpty()) {
            log.info("No recent call logs found for memberId: {} within the last 7 days.", memberId);
            return;
        }

        // 2. 모든 통화 기록에서 'User'의 대화 전체 추출 및 집계
        StringBuilder aggregatedConversation = new StringBuilder();
        for (CallLog callLog : recentCallLogs) {
            if (callLog.getCallData() != null && !callLog.getCallData().isEmpty()) {
                try {
                    List<ChatMessage> chatHistory = objectMapper.readValue(callLog.getCallData(), new TypeReference<List<ChatMessage>>() {});
                    // 모든 사용자 메시지 추출
                    String userMessages = chatHistory.stream()
                            .filter(m -> "User".equalsIgnoreCase(m.speaker()))
                            .map(ChatMessage::message)
                            .collect(Collectors.joining("\n"));

                    if (!userMessages.isEmpty()) {
                        aggregatedConversation.append(userMessages).append("\n");
                    }
                } catch (JsonProcessingException e) {
                    log.error("Error parsing callData for callLogId: {}", callLog.getCallLogId(), e);
                }
            }
        }

        if (aggregatedConversation.length() == 0) {
            log.info("No valid conversation data found in recent call logs for memberId: {}.", memberId);
            return;
        }

        // 3. KeywordAiService를 사용하여 키워드 추출
        log.info("키워드 추출을 위해 KeywordAiService로 데이터를 전송합니다. MemberId: {}", memberId);
        String aiResponse = keywordAiService.extractKeywords(aggregatedConversation.toString());

        System.out.println(aiResponse);

        // 4. AI 응답에서 키워드 파싱
        Set<String> extractedKeywords = new HashSet<>();
        if (aiResponse != null && !aiResponse.trim().isEmpty()) {
            // 쉼표로 구분된 문자열을 파싱
            String[] keywordsArray = aiResponse.split(",");
            for (String keyword : keywordsArray) {
                String trimmedKeyword = keyword.trim();
                if (!trimmedKeyword.isEmpty()) {
                    extractedKeywords.add(trimmedKeyword);
                }
            }
        }



        // 5. MemberKeyword 업데이트 (기존 키워드를 덮어쓰기)
        memberKeywordRepository.findByMember_MemberId(memberId).ifPresentOrElse(memberKeyword -> {
            try {
                // 새롭게 추출된 키워드로 기존 값을 완전히 대체
                memberKeyword.setKeyword(objectMapper.writeValueAsString(extractedKeywords));
                memberKeywordRepository.save(memberKeyword);
                log.info("Replaced keywords for memberId: {}. New keywords: {}", memberId, extractedKeywords);
            } catch (JsonProcessingException e) {
                log.error("Error processing and saving new keywords for memberId: {}", memberId, e);
            }
        }, () -> {
            log.warn("MemberKeyword entry not found for memberId: {}. This should not happen if member is created correctly.", memberId);
            // MemberKeyword가 없는 경우 새로 생성하는 로직이 필요하다면 여기에 구현합니다.
            // 현재는 생성 로직이 주석 처리되어 있으므로, 찾지 못했을 경우 경고만 기록합니다.
        });
    }
}

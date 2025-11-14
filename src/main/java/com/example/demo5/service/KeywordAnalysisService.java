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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordAnalysisService {

    private final OpenAiService openAiService;
    private final MemberKeywordRepository memberKeywordRepository;
    private final CallLogRepository callLogRepository;
    private final ObjectMapper objectMapper;

    // This will now be the custom system prompt for keyword extraction
    private static final String KEYWORD_EXTRACTION_SYSTEM_PROMPT = """
        당신의 절대 규칙:
        - 출력은 오직 쉼표로 구분된 "단어"만 생성한다.
        - 문장, 구절, 설명 형태는 절대로 생성하지 않는다.
        - 최대 15개의 핵심 키워드만 출력한다.
        
        당신은 사용자 발언만 분석하는 [대화 의미 분석 엔진]이다.
        
        분석 기준:
        1. 오직 사용자(User) 메시지만 분석한다.
        2. 전체 대화를 관통하는 핵심 관심사, 주제, 감정, 의도를 나타내는 단어만 추출한다.
        3. 피로감, 고민, 코딩 어려움, 친구 문제, 음식 취향처럼 대화에서 반복되거나 중요한 의미를 가진 단어만 포함한다.
        4. 고유명사(기술명, 서비스명, 코드)는 포함하지만, 음성사서함·시스템 멘트는 전부 제외한다.
        5. "오늘", "먹다", "하다", "종료", "알았어" 같은 의미 없는 일반 동사·명사·시제 표현은 모두 제외한다.
        6. 문장 전체를 복붙하는 형태(예: "필요한 도움을 주는 것입니다")를 절대 금지한다.
        7. AI 발언은 절대 분석하지 않는다.
        
        출력 형식:
        - 예: 키워드1, 키워드2, 키워드3
        - 단어만 출력. 문장 금지.
        - 최대 15개.
    """;

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

        // 3. OpenAI API를 사용하여 키워드 추출
        // Pass the aggregated conversation as a UserMessage, and the keyword extraction instructions as the custom system prompt
        List<ChatMessage> messages = List.of(new ChatMessage("User", "대화:\n" + aggregatedConversation.toString()));
        log.info("키워드 추출을 위해 OpenAI로 데이터를 전송합니다. 메시지: {}, 시스템 프롬프트: {}", messages, KEYWORD_EXTRACTION_SYSTEM_PROMPT);
        String aiResponse = openAiService.getChatResponse(
                messages,
                KEYWORD_EXTRACTION_SYSTEM_PROMPT
        );

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

        // 5. MemberKeyword 업데이트
        memberKeywordRepository.findByMember_MemberId(memberId).ifPresentOrElse(memberKeyword -> {
            try {
                // 기존 키워드 로드
                Set<String> existingKeywords = objectMapper.readValue(memberKeyword.getKeyword(), new TypeReference<Set<String>>() {});
                // 새 키워드 추가 (중복 제거)
                existingKeywords.addAll(extractedKeywords);
                // 업데이트된 키워드를 JSON 문자열로 저장
                memberKeyword.setKeyword(objectMapper.writeValueAsString(existingKeywords));
                memberKeywordRepository.save(memberKeyword);
                log.info("Updated keywords for memberId: {}. New keywords: {}", memberId, existingKeywords);
            } catch (JsonProcessingException e) {
                log.error("Error processing existing keywords for memberId: {}", memberId, e);
            }
        }, () -> {
            log.warn("MemberKeyword entry not found for memberId: {}. Creating a new one.", memberId);
            // MemberKeyword가 없는 경우 새로 생성 (이 경우는 createMember에서 이미 생성되므로 발생하지 않아야 함)
            // 하지만 방어적으로 코드를 작성
            // Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("Member not found"));
            // MemberKeyword newMemberKeyword = new MemberKeyword();
            // newMemberKeyword.setMember(member);
            // try {
            //     newMemberKeyword.setKeyword(objectMapper.writeValueAsString(extractedKeywords));
            //     memberKeywordRepository.save(newMemberKeyword);
            // } catch (JsonProcessingException e) {
            //     log.error("Error creating new MemberKeyword for memberId: {}", memberId, e);
            // }
        });
    }
}

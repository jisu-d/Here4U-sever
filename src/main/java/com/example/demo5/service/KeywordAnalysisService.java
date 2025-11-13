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
            당신은 주어진 대화에서 핵심 키워드를 추출하는 전문가입니다.
            대화는 사용자(User)와 AI의 상호작용으로 구성됩니다.
            특히 사용자가 중요하게 생각하거나 자주 언급하는 주제, 감정, 핵심 단어들을 중심으로 추출해주세요.
            추출된 키워드는 쉼표로 구분된 문자열 형태로 제공해주세요.
            예시: \"키워드1, 키워드2, 키워드3\"
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

        // 2. 모든 통화 기록에서 첫 번째 대화 추출 및 집계
        StringBuilder aggregatedConversation = new StringBuilder();
        for (CallLog callLog : recentCallLogs) {
            if (callLog.getCallData() != null && !callLog.getCallData().isEmpty()) {
                try {
                    List<ChatMessage> chatHistory = objectMapper.readValue(callLog.getCallData(), new TypeReference<List<ChatMessage>>() {});
                    // 첫 번째 사용자 메시지와 AI 응답 추출
                    String firstUserMessage = chatHistory.stream()
                            .filter(m -> "User".equalsIgnoreCase(m.speaker()))
                            .map(ChatMessage::message)
                            .findFirst()
                            .orElse("");
                    String firstAiResponse = chatHistory.stream()
                            .filter(m -> "AI".equalsIgnoreCase(m.speaker()))
                            .map(ChatMessage::message)
                            .findFirst()
                            .orElse("");

                    if (!firstUserMessage.isEmpty()) {
                        aggregatedConversation.append("User: ").append(firstUserMessage).append("\n");
                    }
                    if (!firstAiResponse.isEmpty()) {
                        aggregatedConversation.append("AI: ").append(firstAiResponse).append("\n");
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
        String aiResponse = openAiService.getChatResponse(
                List.of(new ChatMessage("User", "대화:\n" + aggregatedConversation.toString())),
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

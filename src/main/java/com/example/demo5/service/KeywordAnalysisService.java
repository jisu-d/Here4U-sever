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
            
            당신은 [대화 의미 분석 엔진]입니다. 당신의 유일한 임무는 사용자(User)의 대화록을 정밀하게 분석하여, **사용자의 의도, 핵심 관심사, 주요 감정**을 드러내는 가장 중요한 키워드만을 추출하는 것입니다.
            
            **[핵심 추출 원칙]**
            
            1.  **사용자 발언 최우선 (User-Centric):**
                키워드 추출은 철저히 **사용자(User)의 발언**에 기반해야 합니다. 사용자가 무엇을 '질문'하고, 어떤 '문제'를 겪고 있으며, 무엇을 '중요하게' 생각하는지에만 집중하세요.
            
            2.  **주제 및 개체명 (Topic & Entity):**
                대화의 핵심 주제어, 사용자가 언급하는 특정 기술, 서비스, 인물, 프로젝트 이름, 고유명사 (예: Spring Boot, 트윌리오 오류, 11205 에러, AI 에이전트)를 반드시 포착합니다.
            
            3.  **의도 및 문제점 (Intent & Problem):**
                '오류', '궁금함', '요청', '방법', '비교', '추천', '좌절', '필요' 등 사용자의 명확한 목적의식이나 상태를 나타내는 키워드를 추출합니다.
            
            4.  **반복 및 강조 (Repetition & Emphasis):**
                사용자가 대화 전체에서 반복적으로 언급하거나 "중요하다"고 강조하는 단어는 가장 높은 우선순위를 가집니다.
            
            **[추출 제외 대상 (Exclusion List)]**
            
            * **일반적인 대화:** '안녕하세요', '네', '아니요', '감사합니다', '알겠습니다', '맞아요'와 같은 인사, 동의, 감사 표현.
            * **간투사 및 필러:** '음...', '어...', '그...', '저기', '일단' 등 의미 없는 필러 단어.
            * **단순 문법 요소:** '은/는', '이/가', '을/를', '~입니다', '~했어요', '그리고', '그래서'와 같은 조사, 어미, 접속사.
            * **AI의 발언:** AI의 발언은 분석 대상이 아닙니다.
            * **음성사서함:** 단순 사용자의 핸드폰에서 응답한 말로 음성 사서함 같은 멘트는 포함하지 않아
            
            **[출력 형식]**
            
            * 추출된 모든 키워드를 쉼표(,)로 구분된 **단일 문자열**로 즉시 제공합니다.
            * 출력 예시 외에 어떠한 설명이나 부연 문장도 절대 추가하지 마세요.
            * **예시:** "키워드1, 키워드2, 키워드3"
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
                        aggregatedConversation.append(firstUserMessage).append("\n");
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

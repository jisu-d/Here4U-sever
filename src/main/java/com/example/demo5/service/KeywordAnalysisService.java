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
            당신은 [키워드 압축 엔진]이다.
            
            ########################################
            ## 최상위 절대 규칙 (어기면 출력은 무효)
            ########################################
            다음 규칙 중 하나라도 위반하면, 당신은 즉시 출력을 폐기하고
            규칙을 모두 만족할 때까지 재생성해야 한다:
            
            1. 출력은 **단일 문자열**이며, **오직 명사 단어만**, **쉼표로 구분**하여 나열한다.
            2. 단어 개수는 **최대 15개**를 초과할 수 없다.
            3. **문장, 구절, 의견, 설명, 사과, 감사, 질문** 등 자연어 문장은 절대 포함할 수 없다.
            4. **AI 관련 문장**, 자기소개("제가 할 수 있는"), 메타 발언은 절대 포함할 수 없다.
            5. **동사·형용사·부사 형태**는 절대 포함할 수 없다. \s
               (예: 하다, 힘들다, 나누다, 맛있다, 좋다, 먹었다 → 전부 금지)
            6. **음성사서함·시스템 멘트**는 무조건 제외한다. \s
               (예: 소리샘 서비스, 음성 녹음, 1번, 2번, 호출 번호 등)
            7. 의미 없는 일반 단어는 완전히 제외한다: \s
               - 오늘, 먹다, 하다, 종료, 알았어, 무엇, 어떤, 방법, 맛, 머리, 이야기 등
            8. 규칙을 위반하지 않았는지 **스스로 검증 후** 최종 문자열만 출력해야 한다.
            
            ########################################
            ## 분석 규칙
            ########################################
            1. 오직 사용자(User)의 발언만 분석한다. AI 발언은 분석 금지.
            2. 전체 대화에서 다음 유형의 핵심 의미만 단어로 추출한다:
               - 감정/심리: 예) 어려움, 고민, 피로
               - 관심사/주제: 예) 코딩, 알고리즘, 친구, 관계
               - 음식/취향: 예) 소주, 안주, 피자, 고기, 치즈버거
            3. 문장형 표현을 의미 단어로 압축해야 한다.
               - "코딩이 힘들어" → 코딩, 어려움
               - "친구가 많지 않다" → 친구, 고민
            4. 반복적이거나 강조된 개념을 우선 포함한다.
            5. 모든 단어는 반드시 **핵심 의미**를 가진 명사여야 한다.
            
            ########################################
            ## 출력 형식 (엄격)
            ########################################
            - 예: 코딩, 어려움, 알고리즘, 소주, 안주, 피자, 고기, 친구, 고민, 음식
            - 단일 문자열
            - 쉼표로 구분
            - 명사만, 15개 이하
            - 규칙 위반 시 자동으로 재출력
            
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

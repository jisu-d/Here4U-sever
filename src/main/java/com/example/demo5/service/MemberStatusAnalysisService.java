package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import com.example.demo5.entity.CallLog;
import com.example.demo5.entity.MemberStatus;
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.MemberStatusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberStatusAnalysisService {

    private final OpenAiService openAiService;
    private final MemberStatusRepository memberStatusRepository;
    private final CallLogRepository callLogRepository;
    private final ObjectMapper objectMapper;

    private static final String MEMBER_STATUS_SYSTEM_PROMPT = """
            당신은 사용자의 통화 대화를 분석하여 현재 심리 상태를 "안전", "주의", "확인 필요" 중 하나의 태그로 분류하는 전문가입니다.
            대화는 사용자(User)와 AI의 상호작용으로 구성됩니다.
            각 상태 태그의 기준은 다음과 같습니다:
            - "안전": 사용자가 긍정적이거나 안정적인 감정을 표현하며, 특별한 우려 사항이 감지되지 않습니다. 일상적인 대화가 주를 이룹니다.
            - "주의": 사용자가 약간의 외로움, 스트레스, 불안감, 또는 가벼운 부정적인 감정을 표현합니다. 직접적인 위험은 없지만 지속적인 관심이 필요해 보입니다.
            - "확인 필요": 사용자가 심각한 우울감, 극심한 외로움, 자살 암시, 무기력감, 또는 기타 즉각적인 개입이나 확인이 필요한 심각한 심리적 어려움을 표현합니다.
            
            분석 결과는 오직 다음 세 가지 단어 중 하나로만 응답해야 합니다: "안전", "주의", "확인 필요".
            다른 어떤 추가적인 설명이나 문장 없이 오직 상태 태그 단어 하나만 출력해주세요.
            """;

    @Transactional
    public void analyzeAndSaveMemberStatus(String memberId, LocalDateTime analysisEndTime) {
        LocalDateTime analysisStartTime = analysisEndTime.minusDays(7);

        // 1. 지난 7일간의 통화 기록 가져오기
        List<CallLog> recentCallLogs = callLogRepository.findByMember_MemberIdAndRequestedAtBetween(memberId, analysisStartTime, analysisEndTime);

        if (recentCallLogs.isEmpty()) {
            log.info("No recent call logs found for memberId: {} within the last 7 days. Setting default status '안전'.", memberId);
            updateMemberStatus(memberId, "안전"); // Default to "안전" if no call data
            return;
        }

        // 2. 모든 통화 기록에서 전체 대화 추출 및 집계
        StringBuilder aggregatedConversation = new StringBuilder();
        for (CallLog callLog : recentCallLogs) {
            if (callLog.getCallData() != null && !callLog.getCallData().isEmpty()) {
                try {
                    List<ChatMessage> chatHistory = objectMapper.readValue(callLog.getCallData(), new TypeReference<List<ChatMessage>>() {});
                    for (ChatMessage message : chatHistory) {
                        aggregatedConversation.append(message.speaker()).append(": ").append(message.message()).append("\n");
                    }
                } catch (JsonProcessingException e) {
                    log.error("Error parsing callData for callLogId: {}", callLog.getCallLogId(), e);
                }
            }
        }

        if (aggregatedConversation.length() == 0) {
            log.info("No valid conversation data found in recent call logs for memberId: {}. Setting default status '안전'.", memberId);
            updateMemberStatus(memberId, "안전"); // Default to "안전" if no valid conversation
            return;
        }

        // 3. OpenAI API를 사용하여 상태 분석
        String aiResponse = openAiService.getChatResponse(
                List.of(new ChatMessage("User", "다음 대화를 분석하여 사용자의 심리 상태를 판단해주세요:\n" + aggregatedConversation.toString())),
                MEMBER_STATUS_SYSTEM_PROMPT
        );

        // 4. AI 응답에서 상태 태그 파싱 및 검증
        String determinedStatus = "확인 필요"; // Default to "확인 필요" in case of unexpected response
        if (aiResponse != null && !aiResponse.trim().isEmpty()) {
            String trimmedResponse = aiResponse.trim();
            Set<String> validStatuses = Set.of("안전", "주의", "확인 필요");
            if (validStatuses.contains(trimmedResponse)) {
                determinedStatus = trimmedResponse;
            } else {
                log.warn("OpenAI returned an unexpected status: '{}' for memberId: {}. Defaulting to '확인 필요'.", trimmedResponse, memberId);
            }
        } else {
            log.warn("OpenAI returned empty or null response for memberId: {}. Defaulting to '확인 필요'.", memberId);
        }

        // 5. MemberStatus 업데이트
        updateMemberStatus(memberId, determinedStatus);
    }

    @Transactional
    private void updateMemberStatus(String memberId, String statusTag) {
        memberStatusRepository.findByMember_MemberId(memberId).ifPresentOrElse(memberStatus -> {
            memberStatus.setStatusTag(statusTag);
            memberStatusRepository.save(memberStatus);
            log.info("Updated member status for memberId: {} to '{}'.", memberId, statusTag);
        }, () -> {
            log.error("MemberStatus entry not found for memberId: {}. Cannot update status.", memberId);
            // This case should ideally not happen if MemberStatus is created with Member
        });
    }
}

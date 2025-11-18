package com.example.demo5.service;

import com.example.demo5.dto.ChatMessage;
import com.example.demo5.entity.CallLog;
import com.example.demo5.repository.CallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ConversationSummaryService {

    private final CallLogRepository callLogRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            당신은 대화 요약 전문가입니다.
            주어진 대화 내용을 분석하여, 전체 맥락을 포괄하는 핵심적인 내용으로 한줄의 짧은 문장으로 요약해주세요.
            반드시 한국어로 요약해야 합니다. 최대 글자수 제한은 30자 입니다.
            """;

    @Transactional(readOnly = true)
    public String getConversationSummary(String memberId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);

        List<CallLog> recentCallLogs = callLogRepository.findByMember_MemberIdAndRequestedAtBetween(memberId, startTime, endTime);

        if (recentCallLogs.isEmpty()) {
            return "최근 7일간의 대화 기록이 없습니다.";
        }

        StringBuilder aggregatedConversation = new StringBuilder();
        for (CallLog callLog : recentCallLogs) {
            if (callLog.getCallData() != null && !callLog.getCallData().isEmpty()) {
                try {
                    List<ChatMessage> chatHistory = objectMapper.readValue(callLog.getCallData(), new TypeReference<List<ChatMessage>>() {});
                    String conversationText = chatHistory.stream()
                            .map(m -> m.speaker() + ": " + m.message())
                            .collect(Collectors.joining("\n"));
                    aggregatedConversation.append(conversationText).append("\n\n");
                } catch (JsonProcessingException e) {
                    log.error("Error parsing callData for callLogId: {}", callLog.getCallLogId(), e);
                }
            }
        }

        if (aggregatedConversation.isEmpty()) {
            return "분석할 대화 내용이 없습니다.";
        }

        List<ChatMessage> messages = List.of(new ChatMessage("User", aggregatedConversation.toString()));
        return openAiService.getChatResponse(messages, SUMMARY_SYSTEM_PROMPT);
    }
}

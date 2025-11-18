package com.example.demo5.service;

import com.example.demo5.dto.analysis.AnalysisResponse;
import com.example.demo5.dto.call.CreateCallResponse;
import com.example.demo5.dto.call.CallHistoryResponse;
import com.example.demo5.dto.call.LatestCallStatusResponse;
import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.dto.member.MemberStatusTagResponse;
import com.example.demo5.dto.schedule.CreateScheduleResponse;
import com.example.demo5.dto.schedule.ScheduleRequest;
import com.example.demo5.dto.schedule.UpdateScheduleResponse;
import com.example.demo5.entity.CallLog;
import com.example.demo5.entity.CallSchedule;
import com.example.demo5.entity.Member;
import com.example.demo5.entity.MemberKeyword;
import com.example.demo5.entity.MemberStatus;
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.CallScheduleRepository;
import com.example.demo5.repository.MemberKeywordRepository;
import com.example.demo5.repository.MemberRepository;
import com.example.demo5.repository.MemberStatusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final MemberStatusRepository memberStatusRepository;
    private final CallLogRepository callLogRepository;
    private final CallScheduleRepository callScheduleRepository;
    private final TwilioService twilioService;
    private final ConversationSummaryService conversationSummaryService;
    private final ObjectMapper objectMapper;

    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();


    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        String newMemberId;
        do {
            newMemberId = generateRandomId(5);
        } while (memberRepository.existsById(newMemberId));

        Member newMember = new Member();
        newMember.setMemberId(newMemberId);
        newMember.setPhoneNumber(request.getPhoneNumber());
        Member savedMember = memberRepository.save(newMember);

        // Create and save initial MemberKeyword
        MemberKeyword memberKeyword = new MemberKeyword();
        memberKeyword.setMember(savedMember);
        memberKeyword.setKeyword("[]"); // Initial empty JSON array
        memberKeywordRepository.save(memberKeyword);

        // Create and save initial MemberStatus
        MemberStatus memberStatus = new MemberStatus();
        memberStatus.setMember(savedMember);
        memberStatus.setStatusTag("안전"); // Initial status tag
        memberStatusRepository.save(memberStatus);

        return new MemberResponse(savedMember);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getMemberAnalysis(String memberId) {
        MemberKeyword memberKeyword = memberKeywordRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원 키워드 정보를 찾을 수 없습니다: " + memberId));

        String analysisJson = memberKeyword.getKeyword();

        if (analysisJson == null || analysisJson.trim().isEmpty() || analysisJson.equals("[]")) {
            log.warn("회원 ID {}에 대한 분석 데이터가 비어있습니다.", memberId);
            return new AnalysisResponse("분석 데이터 없음", "정보 없음", "아직 분석된 대화 내용이 없습니다.", "해당 없음");
        }

        try {
            return objectMapper.readValue(analysisJson, AnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("DB의 분석 데이터를 파싱하는 데 실패했습니다. memberId: {}, data: {}", memberId, analysisJson, e);
            return new AnalysisResponse("데이터 파싱 오류", "오류", "저장된 분석 데이터를 읽는 중 문제가 발생했습니다.", "오류");
        }
    }

    @Transactional(readOnly = true)
    public MemberStatusTagResponse getMemberStatusTag(String memberId) {
        MemberStatus memberStatus = memberStatusRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원 상태를 찾을 수 없습니다: " + memberId));
        return new MemberStatusTagResponse(memberStatus);
    }

    @Transactional(readOnly = true)
    public String getConversationSummary(String memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        return conversationSummaryService.getConversationSummary(memberId);
    }

    @Transactional(readOnly = true)
    public LatestCallStatusResponse getLatestAutoCallStatus(String memberId) {
        Optional<CallLog> latestAutoCall = callLogRepository.findTopByMember_MemberIdAndCallTypeOrderByRequestedAtDesc(memberId, CallLog.CallType.AUTO);

        if (latestAutoCall.isEmpty()) {
            return new LatestCallStatusResponse("기록 없음", "");
        }

        CallLog callLog = latestAutoCall.get();
        String callResult = callLog.getStatus() == CallLog.CallStatus.COMPLETED ? "완료" : "부재중";
        String time = callLog.getRequestedAt().format(DateTimeFormatter.ofPattern("HH:mm"));

        return new LatestCallStatusResponse(callResult, time);
    }

    @Transactional(readOnly = true)
    public List<CallHistoryResponse> getCallHistory(String memberId) {
        List<CallLog> callLogs = callLogRepository.findTop3ByMember_MemberIdOrderByRequestedAtDesc(memberId);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return callLogs.stream()
                .map(callLog -> CallHistoryResponse.builder()
                        .summaryQuestion(Optional.ofNullable(callLog.getCallResultSentiment()).orElse("기록 없음"))
                        .mood(Optional.ofNullable(callLog.getSimpleSummary()).orElse("기록 없음"))
                        .date(callLog.getRequestedAt().format(dateFormatter))
                        .time(callLog.getRequestedAt().format(timeFormatter))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateCallResponse initiateManualCall(String memberId, String baseUrl) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));
        return initiateCall(member, CallLog.CallType.MANUAL, baseUrl);
    }

    @Async("taskExecutor")
    @Transactional
    public void initiateAutoCall(CallSchedule schedule, String baseUrl) {


        log.info("자동 전화 실행 (Thread: {}): scheduleId={}, memberId={}",
                Thread.currentThread().getName(),
                schedule.getScheduleId(),
                schedule.getMember().getMemberId());
        initiateCall(schedule.getMember(), CallLog.CallType.AUTO, baseUrl);
    }

    private CreateCallResponse initiateCall(Member member, CallLog.CallType callType, String baseUrl) {
        CallLog callLog = CallLog.builder()
                .member(member)
                .callType(callType)
                .status(CallLog.CallStatus.QUEUED)
                .build();
        CallLog savedCallLog = callLogRepository.save(callLog);

        String formattedPhoneNumber = formatPhoneNumber(member.getPhoneNumber());
        String callSid = twilioService.makeCall(formattedPhoneNumber, baseUrl);
        savedCallLog.setCallSid(callSid);
        callLogRepository.save(savedCallLog); // callSid 저장

        return new CreateCallResponse(savedCallLog);
    }

    @Transactional
    public CreateScheduleResponse createCallSchedule(String memberId, ScheduleRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        CallSchedule schedule = CallSchedule.builder()
                .member(member)
                .startDate(request.getStartDate())
                .frequency(request.getFrequency())
                .callTime(request.getCallTime())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        CallSchedule savedSchedule = callScheduleRepository.save(schedule);
        return new CreateScheduleResponse(savedSchedule);
    }

    @Transactional
    public UpdateScheduleResponse updateCallSchedule(String memberId, Long scheduleId, ScheduleRequest request) {
        CallSchedule schedule = callScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 스케줄을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.getMember().getMemberId().equals(memberId)) {
            throw new IllegalStateException("해당 스케줄을 변경할 권한이 없습니다.");
        }

        schedule.update(
                request.getStartDate(),
                request.getFrequency(),
                request.getCallTime(),
                request.getIsActive()
        );
        return new UpdateScheduleResponse(callScheduleRepository.save(schedule));
    }

    private String formatPhoneNumber(String phoneNumber) {
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.startsWith("0")) {
            return "+82" + digitsOnly.substring(1);
        }
        return digitsOnly;
    }

    private String generateRandomId(int length) {
        byte[] buffer = new byte[4];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer).substring(0, length);
    }
}

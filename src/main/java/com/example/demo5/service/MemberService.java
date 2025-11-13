package com.example.demo5.service;

import com.example.demo5.dto.call.CreateCallRequest;
import com.example.demo5.dto.call.CreateCallResponse;
import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.dto.member.MemberKeywordResponse;
import com.example.demo5.dto.member.MemberStatusTagResponse;
import com.example.demo5.dto.schedule.ScheduleRequest;
import com.example.demo5.dto.schedule.CreateScheduleResponse;
import com.example.demo5.dto.schedule.UpdateScheduleResponse;
import com.example.demo5.entity.CallLog;
import com.example.demo5.entity.CallSchedule;
import com.example.demo5.entity.Member;
import com.example.demo5.entity.MemberKeyword; // New import
import com.example.demo5.entity.MemberStatus; // New import
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.CallScheduleRepository;
import com.example.demo5.repository.MemberKeywordRepository; // New import
import com.example.demo5.repository.MemberRepository;
import com.example.demo5.repository.MemberStatusRepository; // New import
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom; // 랜덤 ID 생성용
import java.util.Base64; // 랜덤 ID 생성용

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository; // New injection
    private final MemberStatusRepository memberStatusRepository; // New injection
    private final CallLogRepository callLogRepository;
    private final CallScheduleRepository callScheduleRepository;
    private final TwilioService twilioService;
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
    public MemberKeywordResponse getMemberKeyword(String memberId) {
        MemberKeyword memberKeyword = memberKeywordRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원 키워드를 찾을 수 없습니다: " + memberId));
        return new MemberKeywordResponse(memberKeyword);
    }

    @Transactional(readOnly = true)
    public MemberStatusTagResponse getMemberStatusTag(String memberId) {
        MemberStatus memberStatus = memberStatusRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원 상태를 찾을 수 없습니다: " + memberId));
        return new MemberStatusTagResponse(memberStatus);
    }

    @Transactional
    public CreateCallResponse initiateManualCall(String memberId, CreateCallRequest request, String baseUrl) {
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

package com.example.demo5.controller;

import com.example.demo5.dto.analysis.AnalysisResponse;
import com.example.demo5.dto.call.CreateCallResponse;
import com.example.demo5.dto.call.CallHistoryResponse;
import com.example.demo5.dto.call.LatestCallStatusResponse;
import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.dto.member.ConversationSummaryResponse;
import com.example.demo5.dto.member.MemberStatusTagResponse;
import com.example.demo5.dto.schedule.ScheduleRequest;
import com.example.demo5.dto.schedule.CreateScheduleResponse;
import com.example.demo5.dto.schedule.UpdateScheduleResponse;
import com.example.demo5.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members") // 공통 URL
public class MemberController {

    private final MemberService memberService;

    @Value("${server.base-url}")
    private String baseUrl;

    /**
     * 1. 회원 추가 API
     * [POST] /api/members
     */
    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @RequestBody CreateMemberRequest request // JSON 요청을 DTO로 받음
    ) {
        MemberResponse response = memberService.createMember(request);

        // 201 Created 상태 코드와 함께 생성된 회원 정보를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. 수동 전화 걸기 API
     * [POST] /api/members/{memberId}/calls
     */
    @PostMapping("/{memberId}/calls")
    public ResponseEntity<CreateCallResponse> makeManualCall(
            @PathVariable String memberId
    ) {
        CreateCallResponse response = memberService.initiateManualCall(memberId, baseUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * 2-1. 맞춤 주제로 전화 걸기 API
     * [POST] /api/members/{memberId}/custom-calls
     */
    @PostMapping("/{memberId}/custom-calls")
    public ResponseEntity<CreateCallResponse> makeCustomCall(
            @PathVariable String memberId,
            @RequestBody com.example.demo5.dto.call.CustomCallRequest request
    ) {
        CreateCallResponse response = memberService.initiateCustomCall(memberId, request.getTopic(), baseUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 자동 전화 스케줄 등록 API
     * [POST] /api/members/{memberId}/schedules
     */
    @PostMapping("/{memberId}/schedules")
    public ResponseEntity<CreateScheduleResponse> addSchedule(
            @PathVariable String memberId,
            @RequestBody ScheduleRequest request
    ) {
        CreateScheduleResponse response = memberService.createCallSchedule(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 4. 자동 전화 스케줄 변경 API
     * [PATCH] /api/members/{memberId}/schedules/{scheduleId}
     */
    @PatchMapping("/{memberId}/schedules/{scheduleId}")
    public ResponseEntity<UpdateScheduleResponse> updateSchedule(
            @PathVariable String memberId,
            @PathVariable Long scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        UpdateScheduleResponse response = memberService.updateCallSchedule(memberId, scheduleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 회원 대화 분석 (키워드, 감정, 피드백) API
     * [GET] /api/members/{memberId}/analysis
     */
    @GetMapping("/{memberId}/analysis")
    public ResponseEntity<AnalysisResponse> getMemberAnalysis(
            @PathVariable String memberId
    ) {
        AnalysisResponse response = memberService.getMemberAnalysis(memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. 회원 상태 태그 조회 API
     * [GET] /api/members/{memberId}/status
     */
    @GetMapping("/{memberId}/status")
    public ResponseEntity<MemberStatusTagResponse> getMemberStatusTag(
            @PathVariable String memberId
    ) {
        MemberStatusTagResponse response = memberService.getMemberStatusTag(memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 7. 7일간 대화 요약 조회 API
     * [GET] /api/members/{memberId}/summary
     */
    @GetMapping("/{memberId}/summary")
    public ResponseEntity<ConversationSummaryResponse> getConversationSummary(
            @PathVariable String memberId
    ) {
        String summary = memberService.getConversationSummary(memberId);
        ConversationSummaryResponse response = new ConversationSummaryResponse(summary);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. 최근 자동 안부 통화 상태 조회 API
     * [GET] /api/members/{memberId}/latest-auto-call
     */
    @GetMapping("/{memberId}/latest-auto-call")
    public ResponseEntity<LatestCallStatusResponse> getLatestAutoCallStatus(
            @PathVariable String memberId
    ) {
        LatestCallStatusResponse response = memberService.getLatestAutoCallStatus(memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * 9. 최근 통화 기록 3건 조회 API
     * [GET] /api/members/{memberId}/call-history
     */
    @GetMapping("/{memberId}/call-history")
    public ResponseEntity<List<CallHistoryResponse>> getCallHistory(
            @PathVariable String memberId
    ) {
        List<CallHistoryResponse> response = memberService.getCallHistory(memberId);
        return ResponseEntity.ok(response);
    }
}

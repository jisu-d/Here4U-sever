package com.example.demo5.controller;

import com.example.demo5.dto.call.CreateCallRequest;
import com.example.demo5.dto.call.CreateCallResponse;
import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberKeywordResponse;
import com.example.demo5.dto.member.MemberResponse;
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
            @PathVariable String memberId,
            @RequestBody CreateCallRequest request
    ) {
        CreateCallResponse response = memberService.initiateManualCall(memberId, request, baseUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 자동 전화 스케줄 등록 API
     * [POST] /api/members/{memberId}/schedules
     *  {
     *     "startDate": "2025-11-15",
     *     "frequency": "MONTHLY",
     *     "callTime": "19:00:00",
     *     "isActive": true
     * }
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
     * 5. 회원 관심 키워드 조회 API
     * [GET] /api/members/{memberId}/keyword
     */
    @GetMapping("/{memberId}/keyword")
    public ResponseEntity<MemberKeywordResponse> getMemberKeyword(
            @PathVariable String memberId
    ) {
        MemberKeywordResponse response = memberService.getMemberKeyword(memberId);
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
}

package com.example.demo5.controller;

import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members") // 공통 URL
public class MemberController {

    private final MemberService memberService;

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
}
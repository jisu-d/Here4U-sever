package com.example.demo5.dto.member;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateMemberRequest {
    // API로 받을 데이터
    private String phoneNumber;
}
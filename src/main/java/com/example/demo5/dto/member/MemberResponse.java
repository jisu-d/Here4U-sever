package com.example.demo5.dto.member;

import com.example.demo5.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponse {

    private String memberId;
    private String phoneNumber;

    public MemberResponse(Member member) {
        this.memberId = member.getMemberId();
        this.phoneNumber = member.getPhoneNumber();
    }
}
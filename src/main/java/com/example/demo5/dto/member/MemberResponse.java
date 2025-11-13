package com.example.demo5.dto.member;

import com.example.demo5.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponse {

    private String memberId;
    private String phoneNumber;
    private String memberKeyword;
    private String memberStatus;

    public MemberResponse(Member member) {
        this.memberId = member.getMemberId();
        this.phoneNumber = member.getPhoneNumber();
        this.memberKeyword = member.getMemberKeyword();
        this.memberStatus = member.getMemberStatus();
    }
}
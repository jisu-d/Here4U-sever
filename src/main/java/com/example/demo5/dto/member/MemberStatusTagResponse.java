package com.example.demo5.dto.member;

import com.example.demo5.entity.Member;
import lombok.Getter;

@Getter
public class MemberStatusTagResponse {

    private final String memberStatus;

    public MemberStatusTagResponse(Member member) {
        this.memberStatus = member.getMemberStatus();
    }
}

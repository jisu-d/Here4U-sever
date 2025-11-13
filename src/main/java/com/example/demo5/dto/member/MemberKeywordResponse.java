package com.example.demo5.dto.member;

import com.example.demo5.entity.Member;
import lombok.Getter;

@Getter
public class MemberKeywordResponse {

    private final String memberKeyword;

    public MemberKeywordResponse(Member member) {
        this.memberKeyword = member.getMemberKeyword();
    }
}

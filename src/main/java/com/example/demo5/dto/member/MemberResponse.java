package com.example.demo5.dto.member;

import com.example.demo5.entity.Member;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class MemberResponse {

    @JsonProperty("Here4USeverNo")
    private String here4USeverNo;
    private String phoneNumber;

    public MemberResponse(Member member) {
        this.here4USeverNo = member.getMemberId();
        this.phoneNumber = member.getPhoneNumber();
    }
}
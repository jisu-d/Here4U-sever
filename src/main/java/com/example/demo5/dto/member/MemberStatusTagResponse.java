package com.example.demo5.dto.member;

import com.example.demo5.entity.MemberStatus;
import lombok.Getter;

@Getter
public class MemberStatusTagResponse {

    private final String memberStatus;

    public MemberStatusTagResponse(MemberStatus memberStatusEntity) {
        this.memberStatus = memberStatusEntity.getStatusTag();
    }
}

package com.example.demo5.dto.member;

import com.example.demo5.entity.MemberKeyword;
import lombok.Getter;

@Getter
public class MemberKeywordResponse {

    private final String memberKeyword;

    public MemberKeywordResponse(MemberKeyword memberKeywordEntity) {
        this.memberKeyword = memberKeywordEntity.getKeyword();
    }
}

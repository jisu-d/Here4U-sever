package com.example.demo5.dto.member;

import com.example.demo5.entity.MemberKeyword;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j; // Import Slf4j

import java.util.Collections;
import java.util.List;

@Getter
@Slf4j // Add Slf4j annotation
public class MemberKeywordResponse {

    private final List<String> memberKeyword; // Change type to List<String>

    public MemberKeywordResponse(MemberKeyword memberKeywordEntity) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> keywords = Collections.emptyList(); // Initialize with empty list
        try {
            // Parse the JSON string from the entity into a List<String>
            keywords = objectMapper.readValue(memberKeywordEntity.getKeyword(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing memberKeyword JSON for memberId: {}", memberKeywordEntity.getMember().getMemberId(), e);
            // Fallback to empty list or handle as appropriate
        }
        this.memberKeyword = keywords;
    }
}

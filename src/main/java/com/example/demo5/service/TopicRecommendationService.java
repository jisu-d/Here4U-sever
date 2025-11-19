package com.example.demo5.service;

import com.example.demo5.dto.ai.TopicRecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicRecommendationService {

    private final OpenAiService openAiService;

    public TopicRecommendationService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public List<TopicRecommendationResponse> recommendTopics() {
        String customPrompt = """
            당신은 대화 주제를 추천하는 AI입니다.
            사용자가 흥미를 느낄만한 일상적인 대화 주제 3개를 추천해주세요.
            각 주제는 '주요 키워드: 세부 주제' 형식으로 한 줄씩 작성해야 합니다.
            
            세부 주제 같은 경우 최소 10자 최대 15자로 제한 해야해.
            
            주요 키워드 예시: 음악, 여행, 운동, 독서, 패션, 반려동물
            
            출력예시:
            가족: 최근에 가족과 있었던 재미있는 일
            커리어: 현재 직무에서 느끼는 만족감과 어려움
            여행: 지금까지 갔던 여행 중 가장 기억에 남는 곳
            """;

        try {
            // OpenAiService를 통해 AI 응답을 받음
            String rawResponse = openAiService.getChatResponse(Collections.emptyList(), customPrompt);

            // 응답 파싱
            return Arrays.stream(rawResponse.split("\n"))
                    .map(line -> {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            return new TopicRecommendationResponse(parts[0].trim(), parts[1].trim());
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("주제 추천 중 오류 발생: " + e.getMessage());
            // 오류 발생 시 기본 추천 목록 반환
            return List.of(
                    new TopicRecommendationResponse("뉴스", "오늘의 주요 기사"),
                    new TopicRecommendationResponse("건강", "가벼운 스트레칭"),
                    new TopicRecommendationResponse("가족", "자녀 출산 고민")
            );
        }
    }
}

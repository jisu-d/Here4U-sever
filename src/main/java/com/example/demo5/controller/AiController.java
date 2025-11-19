package com.example.demo5.controller;

import com.example.demo5.dto.ai.TopicRecommendationResponse;
import com.example.demo5.service.TopicRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final TopicRecommendationService topicRecommendationService;

    public AiController(TopicRecommendationService topicRecommendationService) {
        this.topicRecommendationService = topicRecommendationService;
    }

    @GetMapping("/recommend-topics")
    public ResponseEntity<List<TopicRecommendationResponse>> recommendTopics() {
        List<TopicRecommendationResponse> topics = topicRecommendationService.recommendTopics();
        return ResponseEntity.ok(topics);
    }
}

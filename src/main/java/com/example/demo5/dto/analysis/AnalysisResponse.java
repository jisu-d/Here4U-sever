package com.example.demo5.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String keywords;
    private String currentMood;
    private String feedback;
    private String summaryQuestion;
}

package com.example.demo5.dto.call;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallHistoryResponse {
    private final String summaryQuestion;
    private final String mood;
    private final String date;
    private final String time;
}

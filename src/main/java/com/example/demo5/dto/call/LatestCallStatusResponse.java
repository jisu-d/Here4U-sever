package com.example.demo5.dto.call;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LatestCallStatusResponse {
    private String callResult; // "완료" 또는 "부재중"
    private String time;   // "10:20" 형식의 시간
}

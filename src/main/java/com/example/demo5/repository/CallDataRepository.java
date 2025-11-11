package com.example.demo5.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

@Repository
public class CallDataRepository {

    // key: callSid, value: 현재 진행중인 질문의 index
    private final Map<String, Integer> callProgress = new ConcurrentHashMap<>();

    // key: callSid, value: {질문: 답변} 형식의 맵 리스트
    private final Map<String, List<Map<String, String>>> surveyResults = new ConcurrentHashMap<>();

    public void startSurvey(String callSid) {
        callProgress.put(callSid, 0);
        surveyResults.put(callSid, new ArrayList<>());
    }

    public int getCurrentQuestionIndex(String callSid) {
        return callProgress.getOrDefault(callSid, 0);
    }

    public void saveAnswerAndProceed(String callSid, String question, String answer) {
        // 답변 저장
        surveyResults.get(callSid).add(Map.of(question, answer));
        // 다음 질문으로 index 증가
        callProgress.computeIfPresent(callSid, (key, index) -> index + 1);
    }

    public List<Map<String, String>> getSurveyResults(String callSid) {
        return surveyResults.get(callSid);
    }

    public void clearCallData(String callSid) {
        callProgress.remove(callSid);
        surveyResults.remove(callSid);
    }
}
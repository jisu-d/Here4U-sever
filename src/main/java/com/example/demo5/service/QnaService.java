package com.example.demo5.service;

import com.example.demo5.repository.CallDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class QnaService {

    // === 설문 질문 목록 (여기서 질문을 수정/추가할 수 있습니다) ===
    private static final List<String> SURVEY_QUESTIONS = List.of(
            "첫 번째 질문입니다. 당신의 이름은 무엇입니까?",
            "두 번째 질문입니다. 가장 좋아하는 색깔은 무엇입니까?",
            "세 번째 질문입니다. 오늘 기분은 어떠신가요?"
    );

    private final TwilioService twilioService;
    private final CallDataRepository callDataRepository;

    public QnaService(TwilioService twilioService, CallDataRepository callDataRepository) {
        this.twilioService = twilioService;
        this.callDataRepository = callDataRepository;
    }

    /**
     * 설문을 시작하는 TwiML을 생성합니다.
     */
    public String startSurvey(String callSid, String ngrokUrl) {
        callDataRepository.startSurvey(callSid);
        String firstQuestion = SURVEY_QUESTIONS.get(0);
        return twilioService.createGatherTwiML(firstQuestion, ngrokUrl);
    }

    /**
     * 사용자의 답변을 처리하고 다음 행동을 결정하는 TwiML을 생성합니다.
     */
    public String processSurveyResponse(String callSid, String speechResult, String ngrokUrl) {
        // 1. 10초 타임아웃 또는 음성인식 실패 시
        if (!StringUtils.hasText(speechResult)) {
            return twilioService.createHangupTwiML("응답이 없어 통화를 종료합니다.");
        }

        // 2. 사용자가 "종료"라고 말했을 시
        if (speechResult.contains("종료")) {
            return twilioService.createHangupTwiML("설문을 중단하고 통화를 종료합니다.");
        }

        // 3. 정상 답변 처리
        int currentQuestionIndex = callDataRepository.getCurrentQuestionIndex(callSid);
        String currentQuestion = SURVEY_QUESTIONS.get(currentQuestionIndex);
        callDataRepository.saveAnswerAndProceed(callSid, currentQuestion, speechResult);

        // 4. 다음 질문이 있는지 확인
        int nextQuestionIndex = callDataRepository.getCurrentQuestionIndex(callSid);
        if (nextQuestionIndex < SURVEY_QUESTIONS.size()) {
            // 다음 질문이 있으면, 다음 질문을 물어봄 (루프)
            String nextQuestion = SURVEY_QUESTIONS.get(nextQuestionIndex);
            return twilioService.createGatherTwiML(nextQuestion, ngrokUrl);
        } else {
            // 모든 질문이 끝났으면, 종료 메시지 후 통화 종료
            System.out.println("최종 설문 결과: " + callDataRepository.getSurveyResults(callSid));
            callDataRepository.clearCallData(callSid); // 저장된 데이터 삭제
            return twilioService.createHangupTwiML("모든 설문이 완료되었습니다. 감사합니다.");
        }
    }
}
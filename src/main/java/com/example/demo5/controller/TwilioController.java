package com.example.demo5.controller;

import com.example.demo5.service.QnaService;
import com.example.demo5.service.TwilioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/twilio")
@RequiredArgsConstructor
public class TwilioController {

    private final TwilioService twilioService;
    private final QnaService qnaService; // MemberService -> QnaService로 변경

    @Value("${server.base-url}")
    private String baseUrl;

    /**
     * 수동/자동 통화 시작 시, 첫 음성 안내를 제공하는 TwiML을 생성합니다.
     */
    @PostMapping(value = "/call/welcome", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> welcome() {
        // QnaService의 startSurvey를 호출하여 첫 질문을 생성하도록 변경할 수 있으나,
        // 현재는 하드코딩된 메시지를 사용하므로 그대로 둡니다.
        String message = "안녕하세요. 히어포유 전화 에이아이 상담 서비스 입니다. 고민이 있으시거나 질문 사항이 있으시면 질문해주세요.";
        String twiML = twilioService.createGatherTwiML(message, baseUrl);
        return ResponseEntity.ok(twiML);
    }

    /**
     * 맞춤 통화 시작 시, AI가 생성한 질문으로 음성 안내 TwiML을 생성합니다.
     */
    @PostMapping(value = "/call/custom-welcome", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> customWelcome(@RequestParam("question") String question) {
        String twiML = twilioService.createGatherTwiML(question, baseUrl);
        return ResponseEntity.ok(twiML);
    }

    /**
     * 사용자의 음성 답변을 처리하고, 다음 행동(AI 응답 또는 종료)을 결정합니다.
     */
    @PostMapping(value = "/call/handle-response", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleResponse(
            @RequestParam("CallSid") String callSid,
            @RequestParam(value = "SpeechResult", required = false) String speechResult
    ) {
        // memberService.continueConversation -> qnaService.processSurveyResponse로 변경
        String twiML = qnaService.processSurveyResponse(callSid, speechResult, baseUrl);
        return ResponseEntity.ok(twiML);
    }

    /**
     * 통화 상태(완료, 실패 등)를 업데이트합니다.
     */
    @PostMapping("/call/status")
    public ResponseEntity<Void> handleCallStatus(
            @RequestParam("CallSid") String callSid,
            @RequestParam("CallStatus") String callStatus
    ) {
        // memberService.updateCallStatus -> qnaService.handleCallTermination으로 변경
        qnaService.handleCallTermination(callSid, callStatus);
        return ResponseEntity.ok().build();
    }
}
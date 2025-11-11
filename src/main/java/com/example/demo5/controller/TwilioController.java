package com.example.demo5.controller;

import com.example.demo5.service.QnaService;
import com.example.demo5.service.TwilioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class TwilioController {

    @Value("https://7255bb2d7c79.ngrok-free.app")
    private String ngrokUrl;

    private final TwilioService twilioService;
    private final QnaService qnaService;

    public TwilioController(TwilioService twilioService, QnaService qnaService) {
        this.twilioService = twilioService;
        this.qnaService = qnaService;
    }

    // 기본 전화 생성하기
    @PostMapping("/api/twilio/call")
    public ResponseEntity<String> makeCall(@RequestParam String to) {
        try {
            twilioService.makeCall(to, ngrokUrl);
            return ResponseEntity.ok("Call initiated to " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to initiate call: " + e.getMessage());
        }
    }

    @PostMapping(value = "/api/twilio/call/welcome", produces = "application/xml")
    public String handleWelcome(HttpServletRequest request) {
        String callSid = request.getParameter("CallSid");
        return qnaService.startSurvey(callSid, ngrokUrl);
    }

    @PostMapping(value = "/api/twilio/call/handle-response", produces = "application/xml")
    public String handleResponse(HttpServletRequest request) {
        String callSid = request.getParameter("CallSid");
        String speechResult = request.getParameter("SpeechResult");
        return qnaService.processSurveyResponse(callSid, speechResult, ngrokUrl);
    }
}
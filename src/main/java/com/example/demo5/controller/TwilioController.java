package com.example.demo5.controller;

import com.example.demo5.service.QnaService;
import com.example.demo5.service.TwilioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/twilio")
public class TwilioController {

    @Value("${server.base-url}")
    private String baseUrl;

    private final TwilioService twilioService;
    private final QnaService qnaService;

    public TwilioController(TwilioService twilioService, QnaService qnaService) {
        this.twilioService = twilioService;
        this.qnaService = qnaService;
    }

    // 기본 전화 생성하기
    @PostMapping("/call")
    public ResponseEntity<String> makeCall(@RequestParam String to) {
        try {
            twilioService.makeCall(to, baseUrl);
            return ResponseEntity.ok("Call initiated to " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to initiate call: " + e.getMessage());
        }
    }

    @PostMapping(value = "/call/welcome", produces = "application/xml")
    public String handleWelcome(HttpServletRequest request) {
        String callSid = request.getParameter("CallSid");
        return qnaService.startSurvey(callSid, baseUrl);
    }

    @PostMapping(value = "/call/handle-response", produces = "application/xml")
    public String handleResponse(HttpServletRequest request) {
        String callSid = request.getParameter("CallSid");
        String speechResult = request.getParameter("SpeechResult");
        return qnaService.processSurveyResponse(callSid, speechResult, baseUrl);
    }

    @PostMapping(value = "/call/status")
    public void handleStatusCallback(HttpServletRequest request) {
        String callSid = request.getParameter("CallSid");
        String callStatus = request.getParameter("CallStatus");
        qnaService.handleCallTermination(callSid, callStatus);
    }
}
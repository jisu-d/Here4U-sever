package com.example.demo5.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Gather;
import com.twilio.twiml.voice.Hangup;
import com.twilio.twiml.voice.Say;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class TwilioService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public String makeCall(String to, String ngrokUrl) {
        String voiceUrl = ngrokUrl + "/api/twilio/call/welcome";

        Call call = Call.creator(
                new PhoneNumber(to),
                new PhoneNumber(twilioPhoneNumber),
                URI.create(voiceUrl)
        ).create();

        return call.getSid();
    }

    /**
     * 메시지를 말하고, 사용자의 음성 입력을 받는 TwiML을 생성합니다.
     */
    public String createGatherTwiML(String message, String ngrokUrl) {
        String gatherUrl = ngrokUrl + "/api/twilio/call/handle-response";

        Say say = new Say.Builder(message).voice(Say.Voice.POLLY_SEOYEON).build();

        Gather gather = new Gather.Builder()
                .inputs(Gather.Input.SPEECH)
                .speechTimeout("1")
                .action(gatherUrl)
                .language(Gather.Language.KO_KR)
                .say(say)
                .build();

        return new VoiceResponse.Builder().gather(gather).build().toXml();
    }

    /**
     * 메시지를 말하고, 통화를 종료하는 TwiML을 생성합니다.
     */
    public String createHangupTwiML(String message) {
        Say say = new Say.Builder(message).voice(Say.Voice.POLLY_SEOYEON).build();
        return new VoiceResponse.Builder().say(say).hangup(new Hangup.Builder().build()).build().toXml();
    }
}
package com.example.demo5.service;

import com.example.demo5.dto.call.CreateCallRequest;
import com.example.demo5.dto.call.CreateCallResponse;
import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.entity.CallLog;
import com.example.demo5.entity.Member;
import com.example.demo5.repository.CallLogRepository;
import com.example.demo5.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom; // 랜덤 ID 생성용
import java.util.Base64; // 랜덤 ID 생성용

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CallLogRepository callLogRepository;
    private final TwilioService twilioService;
    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();


    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {

        // 1. 전화번호 중복 검사
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        // 2. 랜덤 ID 생성 및 중복 확인 (추가된 로직)
        String newMemberId;
        do {
            newMemberId = generateRandomId(5); // 5자리 랜덤 ID 생성
        } while (memberRepository.existsById(newMemberId)); // DB에 ID가 이미 있는지 확인

        // 3. Entity 생성
        Member newMember = new Member();
        newMember.setMemberId(newMemberId); // <-- 생성한 ID를 직접 설정
        newMember.setPhoneNumber(request.getPhoneNumber());

        // 4. DB에 저장
        Member savedMember = memberRepository.save(newMember);

        // 5. DTO로 변환하여 반환
        return new MemberResponse(savedMember);
    }

    @Transactional
    public CreateCallResponse initiateManualCall(String memberId, CreateCallRequest request, String ngrokUrl) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        // 2. 통화 유형 확인
        if (!"MANUAL".equalsIgnoreCase(request.getType())) {
            throw new IllegalArgumentException("지원하지 않는 통화 유형입니다.");
        }

        // 3. 통화 기록(CallLog) 생성 및 저장
        CallLog callLog = CallLog.builder()
                .member(member)
                .callType(CallLog.CallType.MANUAL)
                .status(CallLog.CallStatus.QUEUED)
                .build();
        CallLog savedCallLog = callLogRepository.save(callLog);

        // 4. 전화번호 형식 변환 (e.g., 010-1234-5678 -> +821012345678)
        String formattedPhoneNumber = formatPhoneNumber(member.getPhoneNumber());

        // 5. Twilio를 통해 전화 걸기
        twilioService.makeCall(formattedPhoneNumber, ngrokUrl);

        // 6. 응답 DTO 생성 및 반환
        return new CreateCallResponse(savedCallLog);
    }

    /**
     * 대한민국 전화번호를 E.164 형식으로 변환하는 헬퍼 메서드
     * @param phoneNumber (e.g., "010-1234-5678")
     * @return E.164 formatted phone number (e.g., "+821012345678")
     */
    private String formatPhoneNumber(String phoneNumber) {
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.startsWith("0")) {
            return "+82" + digitsOnly.substring(1);
        }
        // 이미 국제 형식이거나 다른 형식일 경우 일단 그대로 반환 (또는 추가적인 예외 처리)
        return digitsOnly;
    }


    /**
     * 랜덤 영문/숫자 ID 생성 헬퍼 메서드
     * @param length ID 길이 (5자리)
     * @return 랜덤 ID 문자열
     */
    private String generateRandomId(int length) {
        // Apache Commons Lang3의 RandomStringUtils.randomAlphanumeric(5)를 쓰는 게 가장 편합니다.
        // 라이브러리 없이 구현하려면 아래 방법을 사용합니다.

        // 5자리를 만들기 위해 4바이트 랜덤 데이터 생성
        byte[] buffer = new byte[4]; // 4바이트면 5~6자리 Base64 문자열이 나옵니다.
        random.nextBytes(buffer);
        // Base64 URL-safe 문자로 인코딩 후 5자리만 자르기
        return encoder.encodeToString(buffer).substring(0, length);
    }
}

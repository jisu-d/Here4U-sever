package com.example.demo5.service;

import com.example.demo5.dto.member.CreateMemberRequest;
import com.example.demo5.dto.member.MemberResponse;
import com.example.demo5.entity.Member;
import com.example.demo5.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom; // 랜덤 ID 생성용
import java.util.Base64; // 랜덤 ID 생성용

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
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
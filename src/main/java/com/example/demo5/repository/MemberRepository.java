package com.example.demo5.repository;

import com.example.demo5.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {

    // 전화번호로 중복 가입을 방지하기 위한 메서드
    boolean existsByPhoneNumber(String phoneNumber);
}
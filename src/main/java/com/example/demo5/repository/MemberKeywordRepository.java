package com.example.demo5.repository;

import com.example.demo5.entity.MemberKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {
    Optional<MemberKeyword> findByMember_MemberId(String memberId);
}

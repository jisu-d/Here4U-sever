package com.example.demo5.repository;

import com.example.demo5.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberStatusRepository extends JpaRepository<MemberStatus, Long> {
    Optional<MemberStatus> findByMember_MemberId(String memberId);
}

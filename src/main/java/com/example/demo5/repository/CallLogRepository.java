package com.example.demo5.repository;

import com.example.demo5.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {
    Optional<CallLog> findByCallSid(String callSid);
    List<CallLog> findByMember_MemberIdAndRequestedAtBetween(String memberId, LocalDateTime start, LocalDateTime end);
}

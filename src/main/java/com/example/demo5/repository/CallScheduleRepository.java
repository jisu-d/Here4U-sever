package com.example.demo5.repository;

import com.example.demo5.entity.CallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CallScheduleRepository extends JpaRepository<CallSchedule, Long> {
    List<CallSchedule> findByIsActive(boolean isActive);

    @Query("SELECT cs FROM CallSchedule cs JOIN FETCH cs.member WHERE cs.isActive = :isActive")
    List<CallSchedule> findActiveSchedulesWithMember(@Param("isActive") boolean isActive);
}

package com.example.demo5.repository;

import com.example.demo5.entity.CallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallScheduleRepository extends JpaRepository<CallSchedule, Long> {
    List<CallSchedule> findByIsActive(boolean isActive);
}

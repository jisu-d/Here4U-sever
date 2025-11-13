package com.example.demo5.repository;

import com.example.demo5.entity.CallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallScheduleRepository extends JpaRepository<CallSchedule, Long> {
}

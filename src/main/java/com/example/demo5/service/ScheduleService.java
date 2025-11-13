package com.example.demo5.service;

import com.example.demo5.dto.schedule.ScheduleRequest;
import com.example.demo5.dto.schedule.UpdateScheduleResponse;
import com.example.demo5.entity.CallSchedule;
import com.example.demo5.repository.CallScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CallScheduleRepository callScheduleRepository;

    @Transactional
    public UpdateScheduleResponse updateSchedule(Long scheduleId, ScheduleRequest request) {
        // 1. 스케줄 조회
        CallSchedule schedule = callScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 스케줄을 찾을 수 없습니다: " + scheduleId));

        // 2. 엔티티 업데이트 (값이 있는 필드만 변경)
        schedule.update(
                request.getStartDate(),
                request.getFrequency(),
                request.getCallTime(),
                request.getIsActive()
        );

        // 3. DB에 저장 (Transactional 어노테이션에 의해 변경 감지되어 자동 저장되지만 명시적으로 호출)
        CallSchedule updatedSchedule = callScheduleRepository.save(schedule);

        // 4. DTO로 변환하여 반환
        return new UpdateScheduleResponse(updatedSchedule);
    }
}

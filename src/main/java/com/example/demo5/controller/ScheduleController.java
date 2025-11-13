package com.example.demo5.controller;

import com.example.demo5.dto.schedule.ScheduleRequest;
import com.example.demo5.dto.schedule.UpdateScheduleResponse;
import com.example.demo5.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 자동 전화 스케줄 변경 API
     * [PATCH] /api/schedules/{scheduleId}
     *  {
     *     "startDate": "2025-11-15",
     *     "frequency": "DAILY", "WEEKLY", "MONTHLY", 매일 매주 매월
     *     "callTime": "19:00:00",
     *     "isActive": true
     * }
     */
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<UpdateScheduleResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleRequest request
    ) {
        UpdateScheduleResponse response = scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(response);
    }
}

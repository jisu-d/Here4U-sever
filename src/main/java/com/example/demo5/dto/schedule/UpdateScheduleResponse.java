package com.example.demo5.dto.schedule;

import com.example.demo5.entity.CallSchedule;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class UpdateScheduleResponse {

    private final Long scheduleId;
    private final LocalDate startDate;
    private final CallSchedule.Frequency frequency;
    @JsonFormat(pattern = "HH:mm:ss")
    private final LocalTime callTime;
    private final boolean isActive;
    private final String message = "스케줄이 성공적으로 변경되었습니다.";

    public UpdateScheduleResponse(CallSchedule schedule) {
        this.scheduleId = schedule.getScheduleId();
        this.startDate = schedule.getStartDate();
        this.frequency = schedule.getFrequency();
        this.callTime = schedule.getCallTime();
        this.isActive = schedule.isActive();
    }
}

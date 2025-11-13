package com.example.demo5.dto.schedule;

import com.example.demo5.entity.CallSchedule;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalTime;

@Getter
public class CreateScheduleResponse {

    private final Long scheduleId;
    private final String message = "성공적으로 생성되었습니다.";
    private final CallSchedule.Frequency frequency;

    @JsonFormat(pattern = "HH:mm:ss")
    private final LocalTime callTime;

    public CreateScheduleResponse(CallSchedule schedule) {
        this.scheduleId = schedule.getScheduleId();
        this.frequency = schedule.getFrequency();
        this.callTime = schedule.getCallTime();
    }
}

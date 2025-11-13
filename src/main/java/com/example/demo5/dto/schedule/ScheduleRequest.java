package com.example.demo5.dto.schedule;

import com.example.demo5.entity.CallSchedule;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleRequest {
    private LocalDate startDate;
    private CallSchedule.Frequency frequency;
    private LocalTime callTime;
    private Boolean isActive;
}

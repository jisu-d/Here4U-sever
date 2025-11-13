package com.example.demo5.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "call_schedule")
public class CallSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "call_time", nullable = false)
    private LocalTime callTime;

    @Column(name = "is_active")
    private boolean isActive;

    public void update(LocalDate startDate, Frequency frequency, LocalTime callTime, Boolean isActive) {
        if (startDate != null) this.startDate = startDate;
        if (frequency != null) this.frequency = frequency;
        if (callTime != null) this.callTime = callTime;
        if (isActive != null) this.isActive = isActive;
    }

    @Builder
    public CallSchedule(Member member, LocalDate startDate, Frequency frequency, LocalTime callTime, boolean isActive) {
        this.member = member;
        this.startDate = startDate;
        this.frequency = frequency;
        this.callTime = callTime;
        this.isActive = isActive;
    }

    public enum Frequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}

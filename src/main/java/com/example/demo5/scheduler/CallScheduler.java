package com.example.demo5.scheduler;

import com.example.demo5.entity.CallSchedule;
import com.example.demo5.repository.CallScheduleRepository;
import com.example.demo5.service.MemberService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallScheduler {

    private final CallScheduleRepository callScheduleRepository;
    private final MemberService memberService;

    @Value("${server.base-url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        log.warn("[CallScheduler] 스케줄러가 성공적으로 생성되었습니다. 1분마다 스케줄 확인을 시작합니다.");
    }

    @Scheduled(cron = "0 * * * * *") // 매 1분마다 실행
    public void checkSchedules() {
        log.warn("========== 스케줄 확인 작업 시작 ==========");
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = LocalDate.now();

        List<CallSchedule> activeSchedules = callScheduleRepository.findByIsActive(true);

        for (CallSchedule schedule : activeSchedules) {
            if (schedule.getStartDate().isAfter(today)) {
                continue;
            }

            if (!schedule.getCallTime().truncatedTo(ChronoUnit.MINUTES).equals(now)) {
                continue;
            }

            boolean shouldCall = false;
            switch (schedule.getFrequency()) {
                case DAILY:
                    shouldCall = true;
                    break;
                case WEEKLY:
                    if (today.getDayOfWeek() == schedule.getStartDate().getDayOfWeek()) {
                        shouldCall = true;
                    }
                    break;
                case MONTHLY:
                    if (today.getDayOfMonth() == schedule.getStartDate().getDayOfMonth()) {
                        shouldCall = true;
                    }
                    break;
            }

            if (shouldCall) {
                try {
                    memberService.initiateAutoCall(schedule, baseUrl);
                } catch (Exception e) {
                    log.error("자동 전화 실행 중 오류 발생: scheduleId={}", schedule.getScheduleId(), e);
                }
            }
        }
        log.warn("========== 스케줄 확인 작업 종료 ==========");
    }
}

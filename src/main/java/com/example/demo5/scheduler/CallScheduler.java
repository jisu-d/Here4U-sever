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
        log.warn("========== 스케줄 확인 작업 시작 (서버 인식 현재 시간: {}) ==========", LocalTime.now());
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = LocalDate.now();

        List<CallSchedule> activeSchedules = callScheduleRepository.findActiveSchedulesWithMember(true);
        log.info("확인할 활성 스케줄 {}개 발견", activeSchedules.size());

        for (CallSchedule schedule : activeSchedules) {
            log.info("--- [확인 중] Schedule ID: {}, 설정된 시간: {}, 설정된 시작일: {}",
                    schedule.getScheduleId(), schedule.getCallTime(), schedule.getStartDate());

            // 조건 1: 시작 날짜 확인
            if (schedule.getStartDate().isAfter(today)) {
                log.info("    └> 결과: 시작 날짜가 되지 않아 건너뜁니다.");
                continue;
            }

            // 조건 2: 시간 확인
            if (!schedule.getCallTime().truncatedTo(ChronoUnit.MINUTES).equals(now)) {
                log.info("    └> 결과: 시간이 일치하지 않아 건너뜁니다. (현재 시간: {})", now);
                continue;
            }

            log.info("    └> 시간 일치! 주기 확인을 시작합니다. (주기: {})", schedule.getFrequency());

            boolean shouldCall = false;
            switch (schedule.getFrequency()) {
                case DAILY:
                    shouldCall = true;
                    break;
                case WEEKLY:
                    if (today.getDayOfWeek() == schedule.getStartDate().getDayOfWeek()) shouldCall = true;
                    break;
                case MONTHLY:
                    if (today.getDayOfMonth() == schedule.getStartDate().getDayOfMonth()) shouldCall = true;
                    break;
            }

            if (shouldCall) {
                log.warn("    └> !!! 모든 조건 만족. Schedule ID: {} 자동 전화 실행을 요청합니다.", schedule.getScheduleId());
                try {
                    memberService.initiateAutoCall(schedule, baseUrl);
                } catch (Exception e) {
                    log.error("자동 전화 실행 중 오류 발생: scheduleId={}", schedule.getScheduleId(), e);
                }
            } else {
                log.info("    └> 결과: 주기가 일치하지 않아 건너뜁니다.");
            }
        }
        log.warn("========== 스케줄 확인 작업 종료 ==========");
    }
}

// Created: 2026-06-08 15:34:44
package com.trip.festival.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job festivalSyncJob;

    @Scheduled(cron = "0 0 6 * * *",  zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    public void syncFestivals() {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("syncAt", LocalDateTime.now())
                .toJobParameters();
        try {
            jobLauncher.run(festivalSyncJob, params);
            log.info("행사 동기화 Job 완료");
        } catch (Exception e) {
            log.error("행사 동기화 Job 실패", e);
        }
    }

    /** 수동 실행 — 개발·운영 강제 동기화용 */
    public void syncNow() {
        syncFestivals();
    }
}

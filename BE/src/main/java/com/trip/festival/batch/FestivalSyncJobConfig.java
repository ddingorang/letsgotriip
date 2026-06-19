// Created: 2026-06-08 15:34:39
package com.trip.festival.batch;

import com.trip.festival.client.TourApiClient;
import com.trip.festival.dto.TourApiResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FestivalSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TourApiClient tourApiClient;
    private final FestivalRepository festivalRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Job festivalSyncJob() {
        return new JobBuilder("festivalSyncJob", jobRepository)
                .start(festivalSyncStep())
                .next(cleanupStep())
                .build();
    }

    @Bean
    public Step festivalSyncStep() {
        return new StepBuilder("festivalSyncStep", jobRepository)
                .<TourApiResponse.FestivalItem, Festival>chunk(100, transactionManager)
                .reader(festivalItemReader())
                .processor(festivalItemProcessor())
                .writer(festivalItemWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean
    public FestivalItemReader festivalItemReader() {
        return new FestivalItemReader(tourApiClient);
    }

    @Bean
    public FestivalItemProcessor festivalItemProcessor() {
        return new FestivalItemProcessor();
    }

    @Bean
    public FestivalItemWriter festivalItemWriter() {
        return new FestivalItemWriter(festivalRepository);
    }

    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 1) end_date 경과 행사 → ENDED
                    int expired = jdbcTemplate.update("""
                            UPDATE festivals
                            SET    status = 'ENDED'
                            WHERE  end_date < CURDATE()
                              AND  status  != 'ENDED'
                            """);
                    log.info("종료 행사 상태 업데이트: {}건 → ENDED", expired);

                    // 2) 이번 sync 에서 수신되지 않은(=TourAPI 에서 사라진) 행사 정리.
                    //    수신된 행사는 saveAll 시 @PreUpdate 로 synced_at 이 갱신되므로,
                    //    이 Job 시작 시각보다 synced_at 이 오래된 행사는 미수신으로 간주해 ENDED 처리한다.
                    //    (하드 삭제 대신 비활성화 — 안전한 방식)
                    LocalDateTime jobStartTime = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getStartTime();

                    if (jobStartTime != null) {
                        int stale = jdbcTemplate.update("""
                                UPDATE festivals
                                SET    status = 'ENDED'
                                WHERE  synced_at < ?
                                  AND  status   != 'ENDED'
                                """, jobStartTime);
                        log.info("미수신(소멸) 행사 정리: {}건 → ENDED", stale);
                    } else {
                        log.warn("Job 시작 시각을 확인할 수 없어 미수신 행사 정리를 건너뜁니다.");
                    }

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}

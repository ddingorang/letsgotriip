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
                    int updated = jdbcTemplate.update("""
                            UPDATE festivals
                            SET    status = 'ENDED'
                            WHERE  end_date < CURDATE()
                              AND  status  != 'ENDED'
                            """);
                    log.info("종료 행사 상태 업데이트: {}건 → ENDED", updated);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}

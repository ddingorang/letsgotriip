// Created: 2026-06-08 15:33:08
package com.trip.festival.batch;

import com.trip.festival.entity.Festival;
import com.trip.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class FestivalItemWriter implements ItemWriter<Festival> {

    private final FestivalRepository festivalRepository;

    @Override
    public void write(Chunk<? extends Festival> chunk) {
        festivalRepository.saveAll(new ArrayList<>(chunk.getItems()));
        log.debug("{}건 upsert 완료", chunk.getItems().size());
    }
}

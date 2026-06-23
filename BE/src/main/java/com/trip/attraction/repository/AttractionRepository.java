package com.trip.attraction.repository;

import com.trip.attraction.entity.Attraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Attraction JPA 리포지토리
 * RepositoryConfig 화이트리스트에 com.trip.attraction.repository 추가 필요
 */
public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    Optional<Attraction> findByContentIdAndContentType(String contentId, Integer contentType);

    // ─────────────────────────────────────────────────────────────
    // 큐레이션 조회 — 태그(구분자 LIKE) + 좋아요순/최신순
    //  태그는 ",food,night," 형태로 저장되므로 LIKE '%,food,%' 가 정확 매칭.
    //  "큐레이션" = tags 가 있는(배치된) 행만 대상. tags 없는 행은 제외.
    // ─────────────────────────────────────────────────────────────

    /** 특정 태그 — 좋아요순 */
    @Query("select a from Attraction a " +
           "where a.tags like concat('%,', :tag, ',%') " +
           "order by a.likeCount desc")
    Page<Attraction> findByTagOrderByLike(@Param("tag") String tag, Pageable pageable);

    /** 특정 태그 — 최신순(fetchedAt desc) */
    @Query("select a from Attraction a " +
           "where a.tags like concat('%,', :tag, ',%') " +
           "order by a.fetchedAt desc")
    Page<Attraction> findByTagOrderByLatest(@Param("tag") String tag, Pageable pageable);

    /** 전체 큐레이션(태그 보유) — 좋아요순 */
    @Query("select a from Attraction a " +
           "where a.tags is not null and a.tags <> '' " +
           "order by a.likeCount desc")
    Page<Attraction> findCuratedOrderByLike(Pageable pageable);

    /** 전체 큐레이션(태그 보유) — 최신순 */
    @Query("select a from Attraction a " +
           "where a.tags is not null and a.tags <> '' " +
           "order by a.fetchedAt desc")
    Page<Attraction> findCuratedOrderByLatest(Pageable pageable);

    /** 태그 보유 전체(reroll 좋아요 재부여용) */
    @Query("select a from Attraction a where a.tags is not null and a.tags <> ''")
    java.util.List<Attraction> findAllTagged();
}

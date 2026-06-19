# Created: 2026-06-16 14:26:29
<template>
  <div class="page">
    <div class="scroll-content">
      <!-- Top icons -->
      <div class="top-bar">
        <div style="flex:1" />
        <button class="icon-btn bell-wrap" @click="$router.push('/notifications')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span v-if="notifStore.hasUnread" class="notif-dot" />
        </button>
        <button class="icon-btn" @click="$router.push('/mypage/edit')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
      </div>

      <!-- Profile -->
      <div class="profile-section">
        <div class="avatar-col">
          <div class="avatar-circle">
            <img v-if="authStore.user?.profileImageUrl" :src="authStore.user.profileImageUrl" :alt="authStore.user.nickname" class="avatar-img" />
            <span v-else class="avatar-text">프로필</span>
          </div>
          <div class="camera-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" /><circle cx="12" cy="13" r="4" />
            </svg>
          </div>
        </div>
        <div class="profile-info">
          <h2 class="profile-name">{{ authStore.user?.nickname ?? '프로필' }}</h2>
          <p class="profile-bio">{{ authStore.user?.bio || authStore.user?.email || '' }}</p>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.plans ?? planCount }}</span>
          <span class="stat-label">여행 계획</span>
        </div>
        <div class="stat-divider" />
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.completedPlans ?? completedCount }}</span>
          <span class="stat-label">다녀온 여행</span>
        </div>
        <div class="stat-divider" />
        <div class="stat">
          <span class="stat-num">{{ gami?.stats?.badges ?? unlockedBadgeCount }}</span>
          <span class="stat-label">뱃지</span>
        </div>
      </div>

      <!-- Challenge card -->
      <div class="challenge-card" @click="$router.push('/mypage/challenge')">
        <div class="challenge-header">
          <div class="challenge-left">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="var(--color-peach)" stroke="none">
              <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
            </svg>
            <span class="challenge-title">이달의 챌린지 · {{ gami?.challenge?.month ?? '' }}</span>
          </div>
          <span class="challenge-phase">{{ gami?.challenge?.current ?? 0 }} / {{ gami?.challenge?.goal ?? 10 }}곳</span>
        </div>
        <div class="challenge-bar">
          <div class="challenge-fill" :style="{ width: (gami?.challenge?.percent ?? 0) + '%' }" />
        </div>
        <p class="challenge-hint">{{ gami?.challenge?.hint ?? '계획을 만들어 챌린지를 시작해보세요!' }}</p>
      </div>

      <!-- Main tabs -->
      <div class="main-tab-bar">
        <button
          v-for="(tab, i) in mainTabs"
          :key="i"
          :class="['main-tab', { active: activeMain === i }]"
          @click="activeMain = i"
        >
          {{ tab }}
        </button>
      </div>

      <!-- ① 내 계획 -->
      <div v-show="activeMain === 0" class="tab-content">
        <div class="sub-filter">
          <button
            v-for="f in planFilters"
            :key="f"
            :class="['sub-chip', { active: planFilter === f }]"
            @click="planFilter = f"
          >
            {{ f }}
          </button>
        </div>

        <template v-if="filteredPlans.length > 0">
          <div
            v-for="plan in filteredPlans"
            :key="plan.id"
            class="plan-item"
            @click="$router.push('/plan')"
          >
            <div class="plan-thumb">
              <span class="plan-thumb-label">{{ plan.thumbLabel }}</span>
            </div>
            <div class="plan-info">
              <div class="plan-title-row">
                <span class="plan-title">{{ plan.title }}</span>
                <span :class="['plan-badge', plan.status === '예정' ? 'badge-upcoming' : 'badge-done']">{{ plan.status }}</span>
              </div>
              <div class="plan-meta">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
                {{ plan.dateRange }}
              </div>
              <button v-if="plan.status === '완료'" class="album-link" @click.stop="$router.push(`/mypage/album/${plan.id}`)">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /></svg>
                앨범 보기
              </button>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </template>

        <div v-else class="empty-plans">
          <div class="empty-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </div>
          <h3 class="empty-title">아직 여행 계획이 없어요</h3>
          <p class="empty-sub">첫 여행을 계획하고<br />나만의 일정을 만들어보세요!</p>
          <button class="create-plan-btn" @click="$router.push('/plan')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            여행 계획 만들기
          </button>
        </div>
      </div>

      <!-- ② 앨범 -->
      <div v-show="activeMain === 1" class="tab-content">
        <!-- Phase 2 lock state -->
        <div v-if="albumPhase2" class="phase2-placeholder">
          <div class="phase2-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="12" cy="12" r="3" /></svg>
            <div class="lock-badge">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2.5" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" /></svg>
            </div>
          </div>
          <span class="phase2-label">Phase 2</span>
          <h3 class="phase2-title">앨범은 곧 만나요</h3>
          <p class="phase2-sub">여행이 끝나면 사진이 자동으로<br />앨범에 정리될 예정이에요.</p>
          <!-- 앨범이 0개여도 직접 만들 수 있게 CTA 노출 -->
          <button class="create-plan-btn phase2-create" :disabled="albumCreating" @click="createAlbum">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            {{ albumCreating ? '만드는 중…' : '앨범 만들기' }}
          </button>
        </div>

        <!-- Albums grid -->
        <div v-else>
          <div class="album-section-header">
            <span class="album-count">앨범 {{ albums.length }}</span>
            <button class="make-album-btn" :disabled="albumCreating" @click="createAlbum">
              {{ albumCreating ? '만드는 중…' : '+ 앨범 만들기' }}
            </button>
          </div>
          <div class="albums-grid">
            <div
              v-for="album in albums"
              :key="album.id"
              class="album-card"
              @click="$router.push(`/mypage/album/${album.id}`)"
            >
              <div class="album-thumb">
                <!-- 썸네일 실데이터: 있으면 그라데이션 위에 덮고, 없으면 폴백 유지 -->
                <img v-if="album.thumbnailUrl" :src="album.thumbnailUrl" :alt="album.title" class="album-thumb-img" />
                <div class="album-photo-count">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                  {{ album.photoCount }}
                </div>
                <span class="album-place-label">{{ album.location }}</span>
              </div>
              <div class="album-info">
                <span class="album-title">{{ album.title }}</span>
                <span class="album-auto">자동</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ③ 뱃지 -->
      <div v-show="activeMain === 2" class="tab-content">
        <div class="badge-header">획득한 뱃지 <strong>{{ gami?.stats?.badges ?? 0 }}</strong> / {{ badgeList.length }}</div>
        <div class="badges-grid">
          <div v-for="badge in badgeList" :key="badge.key" class="badge-item">
            <div :class="['badge-circle', badge.unlocked ? 'unlocked' : 'locked']">
              <svg v-if="badge.unlocked" width="20" height="20" viewBox="0 0 24 24" fill="white" stroke="none"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" /></svg>
            </div>
            <span class="badge-name">{{ badge.name }}</span>
            <span v-if="!badge.unlocked && badge.progressText" class="badge-progress">{{ badge.progressText }}</span>
          </div>
        </div>
      </div>

      <!-- 내 활동 메뉴 -->
      <div class="menu-section">
        <p class="menu-section-title">내 활동</p>

        <!-- 찜 목록 -->
        <button class="menu-row" @click="openFavorites">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
            </svg>
          </span>
          <span class="menu-label">찜 목록</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 내 리뷰 -->
        <button class="menu-row" @click="openReviews">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
            </svg>
          </span>
          <span class="menu-label">내 리뷰</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 내 여행 스토리 -->
        <button class="menu-row" @click="$router.push('/stories')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z" /><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z" />
            </svg>
          </span>
          <span class="menu-label">내 여행 스토리</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 내 그룹 -->
        <button class="menu-row" @click="$router.push('/groups')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
            </svg>
          </span>
          <span class="menu-label">내 그룹</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 팔로잉 -->
        <button class="menu-row" @click="openFollowing">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><line x1="19" y1="8" x2="19" y2="14" /><line x1="22" y1="11" x2="16" y2="11" />
            </svg>
          </span>
          <span class="menu-label">팔로잉</span>
          <span v-if="followingCount > 0" class="menu-count">{{ followingCount }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 체크리스트 -->
        <button class="menu-row" @click="$router.push('/checklist')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 11 12 14 22 4" /><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
            </svg>
          </span>
          <span class="menu-label">체크리스트</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 뱃지 -->
        <button class="menu-row" @click="$router.push('/badges')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="8" r="6" /><path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11" />
            </svg>
          </span>
          <span class="menu-label">뱃지</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>
      </div>

      <!-- AI & 데이터 메뉴 -->
      <div class="menu-section">
        <p class="menu-section-title">AI · 데이터</p>

        <!-- AI 어시스턴트 -->
        <button class="menu-row" @click="$router.push('/assistant')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
            </svg>
          </span>
          <span class="menu-label">AI 어시스턴트</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 데이터 분석·업로드 -->
        <button class="menu-row" @click="$router.push('/analysis')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
            </svg>
          </span>
          <span class="menu-label">데이터 분석·업로드</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>

        <!-- 문서 관리 -->
        <button class="menu-row" @click="$router.push('/documents')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" />
            </svg>
          </span>
          <span class="menu-label">문서 관리</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>
      </div>

      <!-- 운영 관리 (관리자 전용) -->
      <div v-if="isAdmin" class="menu-section">
        <p class="menu-section-title">운영</p>

        <button class="menu-row" @click="$router.push('/admin')">
          <span class="menu-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="M9 12l2 2 4-4" />
            </svg>
          </span>
          <span class="menu-label">운영 관리</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>
      </div>

      <!-- Logout -->
      <div class="logout-section">
        <button class="logout-btn" @click="handleLogout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          로그아웃
        </button>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- 찜 목록 시트 -->
    <div v-if="favSheetOpen" class="sheet-backdrop" @click.self="favSheetOpen = false">
      <div class="sheet">
        <div class="sheet-head">
          <h3 class="sheet-title">찜 목록</h3>
          <button class="sheet-close" @click="favSheetOpen = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>
        <div class="sheet-body">
          <p v-if="favLoading" class="sheet-hint">불러오는 중…</p>
          <p v-else-if="favError" class="sheet-hint">{{ favError }}</p>
          <template v-else-if="favorites.length > 0">
            <div v-for="fav in favorites" :key="fav.id" class="fav-item">
              <button class="fav-main" @click="goFavorite(fav)">
                <span :class="['fav-type', favTypeClass(fav.targetType)]">{{ favTypeLabel(fav.targetType) }}</span>
                <span class="fav-id">#{{ fav.targetId }}</span>
              </button>
              <button class="fav-remove" @click="removeFavorite(fav)" aria-label="찜 해제">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>
          </template>
          <div v-else class="sheet-empty">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" /></svg>
            <p class="sheet-hint">아직 찜한 곳이 없어요.<br />관광지·핫플·게시글에서 하트를 눌러보세요.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 내 리뷰 시트 -->
    <div v-if="reviewSheetOpen" class="sheet-backdrop" @click.self="reviewSheetOpen = false">
      <div class="sheet">
        <div class="sheet-head">
          <h3 class="sheet-title">내 리뷰</h3>
          <button class="sheet-close" @click="reviewSheetOpen = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>
        <div class="sheet-body">
          <div class="sheet-empty">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" /></svg>
            <p class="sheet-hint">관광지 상세 화면에서 별점과 리뷰를 남길 수 있어요.<br />작성한 리뷰는 각 관광지 상세에서 확인·수정할 수 있어요.</p>
            <button class="sheet-cta" @click="reviewSheetOpen = false; $router.push('/explore')">
              관광지 둘러보기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 팔로잉 시트 (GET /api/follows/me/following) -->
    <div v-if="followSheetOpen" class="sheet-backdrop" @click.self="followSheetOpen = false">
      <div class="sheet">
        <div class="sheet-head">
          <h3 class="sheet-title">팔로잉 {{ followingCount > 0 ? followingCount : '' }}</h3>
          <button class="sheet-close" @click="followSheetOpen = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>
        <div class="sheet-body">
          <p v-if="followLoading" class="sheet-hint">불러오는 중…</p>
          <p v-else-if="followError" class="sheet-hint">{{ followError }}</p>
          <template v-else-if="following.length > 0">
            <div v-for="f in following" :key="f.userId" class="follow-item">
              <div class="follow-avatar">
                <img v-if="f.profileImageUrl" :src="f.profileImageUrl" :alt="f.nickname" class="follow-avatar-img" />
                <span v-else class="follow-avatar-text">{{ (f.nickname || '?').charAt(0) }}</span>
              </div>
              <div class="follow-info">
                <span class="follow-name">{{ f.nickname }}</span>
                <span v-if="f.bio" class="follow-bio">{{ f.bio }}</span>
              </div>
            </div>
          </template>
          <div v-else class="sheet-empty">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><line x1="19" y1="8" x2="19" y2="14" /><line x1="22" y1="11" x2="16" y2="11" /></svg>
            <p class="sheet-hint">아직 팔로우한 사람이 없어요.<br />커뮤니티에서 마음에 드는 여행자를 팔로우해보세요.</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { useNotificationStore } from '@/stores/notification.js'
import { useGamificationStore } from '@/stores/gamification.js'
import { http } from '@/api/http.js'
import { favoriteApi, followApi } from '@/api/index.js'

const router = useRouter()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const gamiStore = useGamificationStore()

const gami = computed(() => gamiStore.summary)

// 관리자 여부 — /users/me 응답의 userRole 기준
const isAdmin = computed(() => authStore.user?.userRole === 'ADMIN')

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

const activeMain = ref(0)

// ── 내 계획 (GET /api/plans) ────────────────────────────────────────────────
const planFilters = ['전체', '예정', '완료']
const planFilter = ref('전체')
const allPlans = ref([])

// 날짜 포맷 'M.D' (예: 6.12)
function fmtDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}.${d.getDate()}`
}

// 종료일이 오늘보다 과거면 '완료', 아니면 '예정'
function deriveStatus(endDate) {
  if (!endDate) return '예정'
  const end = new Date(endDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return end < today ? '완료' : '예정'
}

function mapPlan(p) {
  const dateRange = p.startDate && p.endDate
    ? `${fmtDate(p.startDate)}-${fmtDate(p.endDate)}`
    : fmtDate(p.startDate) || fmtDate(p.endDate)
  return {
    id: p.id,
    title: p.title,
    status: deriveStatus(p.endDate),
    dateRange,
    // 썸네일은 일정 제목 앞부분을 사용 (이미지 미보유)
    thumbLabel: (p.title || '여행') + '\n일정',
  }
}

async function loadPlans() {
  try {
    const { data } = await http.get('/api/plans', { params: { page: 0, size: 50 } })
    // Page 응답: { content: [...] } 또는 배열 모두 허용
    const list = Array.isArray(data) ? data : (data?.content ?? [])
    allPlans.value = list.map(mapPlan)
  } catch {
    allPlans.value = []
  }
}

const filteredPlans = computed(() => {
  if (planFilter.value === '전체') return allPlans.value
  return allPlans.value.filter((p) => p.status === planFilter.value)
})

const planCount = computed(() => allPlans.value.length)
const completedCount = computed(
  () => allPlans.value.filter((p) => p.status === '완료').length,
)

// ── 앨범 (GET /users/me/albums) ─────────────────────────────────────────────
const albums = ref([])
// 앨범이 0개면 Phase 2 안내 플레이스홀더를 보여준다.
const albumPhase2 = computed(() => albums.value.length === 0)

async function loadAlbums() {
  try {
    const { data } = await http.get('/users/me/albums')
    const list = Array.isArray(data) ? data : (data?.content ?? [])
    albums.value = list.map((a) => ({
      id: a.id,
      title: a.name,
      location: a.name,
      photoCount: a.photoCount ?? 0,
      thumbnailUrl: a.thumbnailUrl ?? null,
    }))
  } catch {
    albums.value = []
  }
}

// '+ 앨범 만들기' — 이름 입력 후 생성(POST /users/me/albums)하고 목록 갱신.
const albumCreating = ref(false)
async function createAlbum() {
  if (albumCreating.value) return
  const name = (window.prompt('새 앨범 이름을 입력하세요') ?? '').trim()
  if (!name) return
  albumCreating.value = true
  try {
    await http.post('/users/me/albums', { name, imageUrls: [] })
    await loadAlbums()
  } catch (e) {
    window.alert(e.response?.data?.message ?? e.message ?? '앨범을 만들지 못했어요.')
  } finally {
    albumCreating.value = false
  }
}

// ── 뱃지 ────────────────────────────────────────────────────────────────────
// 게임화 API(GET /api/gamification/summary)의 실데이터 뱃지 목록.
// 미로그인/로딩 시 빈 배열 → 화면은 빈 상태로 정직하게 표시.
const badgeList = computed(() => gami.value?.badges ?? [])
const unlockedBadgeCount = computed(
  () => badgeList.value.filter((b) => b.unlocked).length,
)

// 탭 라벨에 실제 개수를 표시
const mainTabs = computed(() => [
  '내 계획',
  `앨범 ${albums.value.length}`,
  `뱃지 ${gami.value?.stats?.badges ?? unlockedBadgeCount.value}`,
])

// ── 찜 목록 시트 (GET /api/favorites) ───────────────────────────────────────
const favSheetOpen = ref(false)
const favorites = ref([])
const favLoading = ref(false)
const favError = ref('')

const FAV_TYPE_LABEL = { ATTRACTION: '관광지', HOTPLACE: '핫플', POST: '게시글' }
function favTypeLabel(t) {
  return FAV_TYPE_LABEL[t] ?? t
}
function favTypeClass(t) {
  return `fav-type-${(t || '').toLowerCase()}`
}

async function openFavorites() {
  favSheetOpen.value = true
  favLoading.value = true
  favError.value = ''
  try {
    const { data } = await favoriteApi.list()
    favorites.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch (e) {
    favError.value = e.response?.data?.message ?? e.message ?? '찜 목록을 불러오지 못했어요.'
    favorites.value = []
  } finally {
    favLoading.value = false
  }
}

function goFavorite(fav) {
  const id = fav.targetId
  favSheetOpen.value = false
  if (fav.targetType === 'ATTRACTION') router.push(`/place/${id}`)
  else if (fav.targetType === 'HOTPLACE') router.push(`/hotplace/${id}`)
  else if (fav.targetType === 'POST') router.push(`/community/${id}`)
}

async function removeFavorite(fav) {
  try {
    await favoriteApi.remove(fav.targetType, fav.targetId)
    favorites.value = favorites.value.filter((f) => f.id !== fav.id)
  } catch (e) {
    favError.value = e.response?.data?.message ?? e.message ?? '찜 해제에 실패했어요.'
  }
}

// ── 내 리뷰 시트 ────────────────────────────────────────────────────────────
// 리뷰는 관광지별로 조회/작성되므로(전체 내 리뷰 목록 엔드포인트 미제공),
// 안내 시트로 관광지 상세로 유도한다.
const reviewSheetOpen = ref(false)
function openReviews() {
  reviewSheetOpen.value = true
}

// ── 팔로잉 시트 (GET /api/follows/me/following) ──────────────────────────────
const followSheetOpen = ref(false)
const following = ref([])
const followLoading = ref(false)
const followError = ref('')
const followingCount = computed(() => following.value.length)

// 마운트 시 카운트 표시용으로 미리 1회 로드(실패는 0개로 정직하게 표시).
async function loadFollowing() {
  followLoading.value = true
  followError.value = ''
  try {
    const { data } = await followApi.following()
    following.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch (e) {
    followError.value = e.response?.data?.message ?? e.message ?? '팔로잉 목록을 불러오지 못했어요.'
    following.value = []
  } finally {
    followLoading.value = false
  }
}

function openFollowing() {
  followSheetOpen.value = true
  loadFollowing()
}

onMounted(() => {
  notifStore.load()
  gamiStore.load()
  loadPlans()
  loadAlbums()
  loadFollowing()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* Top bar */
.top-bar {
  display: flex;
  align-items: center;
  padding: 52px 16px 4px;
}
.icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.bell-wrap { position: relative; }
.notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-peach);
  border: 1.5px solid white;
}

/* Profile */
.profile-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 8px 20px 16px;
}
.avatar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e0d4cc);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.camera-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 1px solid var(--color-line-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-info {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.profile-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.6px;
}
.profile-bio {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

/* Stats */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 12px 20px 16px;
  border-bottom: 1px solid var(--color-line-light);
}
.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.stat-num {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  letter-spacing: -0.1px;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-line-light);
  flex-shrink: 0;
}

/* Challenge card */
.challenge-card {
  margin: 16px 20px;
  padding: 14px 16px;
  background: #fff8f2;
  border: 1px solid #fde8d4;
  border-radius: var(--radius-lg);
  cursor: pointer;
}
.challenge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.challenge-left {
  display: flex;
  align-items: center;
  gap: 7px;
}
.challenge-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.challenge-phase {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}
.challenge-bar {
  height: 5px;
  background: var(--color-line-light);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
}
.challenge-fill {
  height: 100%;
  background: var(--color-peach);
  border-radius: 3px;
}
.challenge-hint {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  letter-spacing: -0.2px;
}

/* Main tabs */
.main-tab-bar {
  display: flex;
  border-bottom: 1px solid var(--color-line-light);
  padding: 0 20px;
}
.main-tab {
  flex: 1;
  padding: 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  position: relative;
  letter-spacing: -0.3px;
}
.main-tab.active {
  color: var(--color-ink);
  font-weight: 700;
}
.main-tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-peach);
  border-radius: 2px 2px 0 0;
}

/* Tab content */
.tab-content { padding: 0 0 20px; }

/* Sub filter chips */
.sub-filter {
  display: flex;
  gap: 8px;
  padding: 14px 20px 8px;
}
.sub-chip {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  transition: all 0.15s;
}
.sub-chip.active {
  background: var(--color-peach);
  color: white;
}

/* Plan items */
.plan-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.plan-thumb {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #ede0d8, #e0d0c8);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.plan-thumb-label {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-align: center;
  white-space: pre-line;
  line-height: 1.4;
}
.plan-info { flex: 1; min-width: 0; }
.plan-title-row {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 5px;
}
.plan-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.plan-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.badge-upcoming { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.badge-done { background: var(--color-surface); color: var(--color-ink-muted); }
.plan-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 4px;
}
.album-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-ink-muted);
  margin-top: 2px;
}

/* Empty plans */
.empty-plans {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 24px;
  text-align: center;
}
.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 6px;
}
.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
  margin-bottom: 24px;
}
.create-plan-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: var(--color-peach);
  color: white;
  font-size: 14.5px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}

/* Album phase 2 */
.phase2-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 24px;
  text-align: center;
}
.phase2-icon {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.lock-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-line-light);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}
.phase2-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 12px;
}
.phase2-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 6px;
}
.phase2-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
}
.phase2-create { margin-top: 20px; }
.phase2-create:disabled { opacity: 0.5; }

/* Albums grid */
.album-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 10px;
}
.album-count {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
}
.make-album-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
}
.make-album-btn:disabled { opacity: 0.5; }
.albums-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 0 20px;
}
.album-card { cursor: pointer; }
.album-thumb {
  aspect-ratio: 1;
  background: linear-gradient(135deg, #ede0d8, #e0d0c8);
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px;
}
/* 썸네일 이미지: 카드 전체를 덮어 그라데이션 위에 표시(배지/라벨은 z-index 로 위에) */
.album-thumb-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.album-photo-count {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(0,0,0,0.55);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 7px;
  border-radius: var(--radius-full);
  align-self: flex-start;
}
.album-place-label {
  position: relative;
  z-index: 1;
  font-size: 11px;
  color: white;
  background: rgba(0,0,0,0.35);
  padding: 2px 8px;
  border-radius: 4px;
  align-self: flex-start;
}
.album-info { display: flex; align-items: center; gap: 6px; }
.album-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.album-auto {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  padding: 2px 7px;
  border-radius: var(--radius-full);
}

/* Badges */
.badge-header {
  padding: 16px 20px 12px;
  font-size: 14px;
  color: var(--color-ink-secondary);
}
.badge-header strong { color: var(--color-ink); font-weight: 700; }
.badges-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 0 20px;
}
.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.badge-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.badge-circle.unlocked { background: var(--color-peach); }
.badge-circle.locked { background: var(--color-surface); border: 1.5px solid var(--color-line-light); }
.badge-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
  text-align: center;
  letter-spacing: -0.2px;
}
.badge-progress {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}

/* 내 활동 / AI·데이터 메뉴 */
.menu-section {
  padding: 4px 0;
  border-top: 1px solid var(--color-line-light);
}
.menu-section-title {
  padding: 14px 20px 4px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}
.menu-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 20px;
  text-align: left;
}
.menu-row:active { background: var(--color-surface); }
.menu-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-label {
  flex: 1;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.logout-section {
  padding: 8px 20px 4px;
  border-top: 1px solid var(--color-line-light);
}
.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 14px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}
.logout-btn:active { opacity: 0.6; }

/* 바텀 시트 (찜 목록 / 내 리뷰) */
.sheet-backdrop {
  position: fixed;          /* .page(overflow:hidden)에 갇히지 않게 뷰포트 기준 */
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 1000;            /* BottomNav(z-index:100) 위로 — 시트 하단이 가려지지 않게 */
}
.sheet {
  width: 100%;
  max-height: 72%;
  display: flex;
  flex-direction: column;
  background: var(--color-white);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  overflow: hidden;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--color-line-light);
}
.sheet-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}
.sheet-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
}
.sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0 20px;
}
.sheet-hint {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
  text-align: center;
  padding: 4px 20px;
}
.sheet-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 24px 24px;
  text-align: center;
}
.sheet-cta {
  margin-top: 4px;
  padding: 12px 24px;
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}
.fav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-line-light);
}
.fav-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  min-width: 0;
}
.fav-type {
  padding: 3px 9px;
  border-radius: var(--radius-full);
  font-size: 11.5px;
  font-weight: 700;
  background: var(--color-surface);
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.fav-type-attraction { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.fav-id {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-remove {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.fav-remove:active { opacity: 0.6; }

/* 메뉴 행 우측 카운트 배지 (팔로잉 등) */
.menu-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 2px 9px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

/* 팔로잉 시트 아이템 */
.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-line-light);
}
.follow-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e0d4cc);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.follow-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.follow-avatar-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink-muted);
}
.follow-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.follow-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.follow-bio {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom-spacer { height: 24px; }
</style>

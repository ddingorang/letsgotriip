export const contentTypeLabels = {
  TOURIST_ATTRACTION: '관광지',
  LODGING: '숙박',
  RESTAURANT: '음식점',
  CULTURAL_FACILITY: '문화시설'
};

export const areaLabels = {
  1: '서울',
  6: '부산',
  32: '경주',
  39: '제주'
};

export const scenes = [
  ['부산 해안', '/backgrounds/trip-bg-01.jpg', '35.1587 N / 129.1604 E', '62% center'],
  ['서울 야경', '/backgrounds/trip-bg-02.jpg', '37.5665 N / 126.9780 E', '58% center'],
  ['제주 산맥', '/backgrounds/trip-bg-03.jpg', '33.4996 N / 126.5312 E', '55% 42%'],
  ['경주 야간 산책', '/backgrounds/trip-bg-04.jpg', '35.8347 N / 129.2196 E', '52% 48%'],
  ['전주 한옥 골목', '/backgrounds/trip-bg-05.jpg', '35.8140 N / 127.1530 E', '48% 52%'],
  ['강릉 소나무 해변', '/backgrounds/trip-bg-06.jpg', '37.7519 N / 128.8761 E', '54% center'],
  ['보성 녹차밭', '/backgrounds/trip-bg-07.jpg', '34.7715 N / 127.0801 E', '50% 45%'],
  ['궁궐의 가을', '/backgrounds/trip-bg-08.jpg', '37.5796 N / 126.9770 E', '52% 46%'],
  ['제주 은빛 오름', '/backgrounds/trip-bg-09.jpg', '33.3617 N / 126.5292 E', '55% 45%'],
  ['담양 대숲길', '/backgrounds/trip-bg-10.jpg', '35.3210 N / 126.9882 E', '50% center']
].map(([label, image, coord, focus]) => ({ label, image, coord, focus }));

export const places = [
  ['place-1001', '해운대해수욕장', 'TOURIST_ATTRACTION', '6', '부산광역시 해운대구', 35.1587, 129.1604, ['바다', '산책']],
  ['place-1002', '경복궁', 'TOURIST_ATTRACTION', '1', '서울특별시 종로구', 37.5796, 126.977, ['역사', '궁궐']],
  ['place-1003', '광안리 해물탕 골목', 'RESTAURANT', '6', '부산광역시 수영구 광안해변로', 35.1532, 129.1186, ['맛집', '해변']],
  ['place-1004', '성산일출봉', 'TOURIST_ATTRACTION', '39', '제주특별자치도 서귀포시 성산읍', 33.4584, 126.9425, ['일출', '트레킹']],
  ['place-1005', '국립중앙박물관', 'CULTURAL_FACILITY', '1', '서울특별시 용산구 서빙고로', 37.524, 126.9803, ['박물관', '실내']],
  ['place-1006', '한옥스테이 감', 'LODGING', '32', '경상북도 경주시 황남동', 35.8347, 129.2196, ['숙박', '한옥']]
].map(([id, title, contentType, areaCode, address, latitude, longitude, tags]) => ({
  id,
  title,
  contentType,
  areaCode,
  address,
  latitude,
  longitude,
  tags
}));

export const placeDetails = {
  'place-1001': '부산을 대표하는 해변 관광지입니다.'
};

export const contextSummary = '2026-06-01 맑음, 18–25°C · 해운대 공영주차장 450m';

export const navItems = [
  ['home', '홈'],
  ['explore', '장소탐색'],
  ['plan', '일정관리'],
  ['recommend', '취향추천'],
  ['hotplace', '핫플검수'],
  ['community', '여행톡'],
  ['mypage', '내여행']
].map(([key, label]) => ({ key, label }));

export const pageWorkflows = {
  home: {
    title: '오늘 이어갈 여행 흐름',
    lead: '저장한 장소, 최근 일정, 다음 작업을 한 화면에서 바로 이어갑니다.',
    detailTitle: '홈에서 이어갈 작업',
    now: [
      ['최근 본 장소', '마지막으로 살펴본 후보를 다시 열어봅니다.'],
      ['저장 장소 정리', '담아둔 장소를 일정 후보로 넘깁니다.'],
      ['다음 작업', '계획, 추천, 공유 중 지금 이어갈 흐름을 고릅니다.']
    ],
    actions: ['장소탐색 열기', '저장 장소 보기', '일정으로 넘기기', '최근 기록 정리']
  },
  plan: {
    title: '지금 점검할 일정',
    lead: '이동 순서, 빈 시간, 공유 전 점검 항목을 먼저 보여줍니다.',
    detailTitle: '일정관리에서 할 작업',
    now: [
      ['부산 1일차 동선', '해운대에서 광안리까지 이동 시간을 확인합니다.'],
      ['비어 있는 오후 슬롯', '카페나 실내 명소를 추가할 수 있습니다.'],
      ['공유 전 점검', '장소 순서와 예상 이동 시간을 확인합니다.']
    ],
    actions: ['장소 추가', '동선 재정렬', '일정 비교', '공유 링크 만들기']
  },
  recommend: {
    title: '취향에 맞는 후보',
    lead: '취향 태그, 날씨, 동행 상황을 기준으로 바로 쓸 수 있는 후보를 정리합니다.',
    detailTitle: '취향추천에서 할 작업',
    now: [
      ['바다 산책 코스', '해변 태그와 저장 이력을 기준으로 추천됩니다.'],
      ['실내 대안', '날씨가 흐릴 때 박물관 후보를 우선합니다.'],
      ['추천 근거', '취향, 지역, 일정 길이를 함께 비교합니다.']
    ],
    actions: ['추천 다시 받기', '추천 근거 보기', '일정으로 복사', '취향 조건 조정']
  },
  hotplace: {
    title: '검수할 새 장소',
    lead: '새 제보와 사진, 좌표 상태를 운영자가 바로 판단할 수 있게 모읍니다.',
    detailTitle: '핫플검수에서 할 작업',
    now: [
      ['신규 제보 3건', '사진과 좌표가 있는 장소부터 검토합니다.'],
      ['내 주변 후보', '현재 선택 지역 근처 핫플을 비교합니다.'],
      ['검수 필요 사진', '중복 장소와 흐린 사진을 정리합니다.']
    ],
    actions: ['핫플 등록', '사진 추가', '좌표 확인', '중복 제보 병합']
  },
  community: {
    title: '여행 이야기 흐름',
    lead: '인기 글, 내 공유 일정, 댓글 상태를 먼저 확인합니다.',
    detailTitle: '여행톡에서 할 작업',
    now: [
      ['이번 주 인기 여행글', '부산 해변 코스 공유글을 먼저 봅니다.'],
      ['중요 공지', '데이터 점검과 이용 안내를 확인합니다.'],
      ['내 공유 일정', '공개 범위와 댓글 상태를 확인합니다.']
    ],
    actions: ['여행글 작성', '공유 일정 보기', '공지 확인', '댓글 관리']
  },
  mypage: {
    title: '내 여행 기록',
    lead: '저장 장소, 취향 태그, 최근 일정에서 다시 이어갈 작업을 보여줍니다.',
    detailTitle: '내여행에서 할 작업',
    now: [
      ['저장 장소 2곳', '최근 담은 장소를 일정으로 넘길 수 있습니다.'],
      ['취향 태그', '바다, 산책, 맛집 태그가 우선 적용됩니다.'],
      ['최근 일정', '부산 일정 편집을 이어갑니다.']
    ],
    actions: ['프로필 수정', '취향 태그 변경', '저장 장소 정리', '내 일정 열기']
  }
};

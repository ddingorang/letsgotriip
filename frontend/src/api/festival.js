// Festival API — BE 경유 래퍼.
// 과거에는 FE가 TourAPI(/api/tour)를 직접 호출하고 서비스키를 하드코딩했으나,
// 이제는 Spring 백엔드(GET /api/festivals)만 호출한다. TourAPI 호출/키 관리는
// 전적으로 BE 책임이다. (festivalApi 정의는 @/api/index.js 참고)
import { festivalApi } from '@/api/index.js'

/**
 * BE 응답(YYYY-MM-DD)을 뷰가 기대하는 YYYYMMDD 형식으로 정규화.
 * BE FestivalResponse.startDate/endDate 는 LocalDate.toString() → "2026-06-19".
 */
function normalizeDate(raw) {
  if (!raw) return ''
  return String(raw).replace(/-/g, '')
}

/**
 * 진행중/예정 축제 목록을 BE에서 조회해 뷰 표준 형태로 매핑한다.
 * @param {{ areaCode?: string, status?: string }} options
 *   - areaCode: 지역 코드(법정동 광역 코드, 예: "11"). 비우면 전체.
 *   - status: UPCOMING | ONGOING | ENDED. 비우면 BE 기본(ENDED 제외).
 * @returns {Promise<Array>} 정규화된 축제 목록
 */
export async function fetchFestivals({ areaCode = '', status = '' } = {}) {
  const params = {}
  if (areaCode) params.areaCode = areaCode
  if (status) params.status = status

  const { data } = await festivalApi.list(params)
  const items = Array.isArray(data) ? data : []

  return items.map((item) => ({
    id: String(item.contentId ?? ''),
    title: String(item.title ?? '').trim(),
    address: String(item.address ?? '').trim(),
    image: String(item.imageUrl ?? ''),
    startDate: normalizeDate(item.startDate),
    endDate: normalizeDate(item.endDate),
    latitude: item.latitude != null ? Number(item.latitude) : null,
    longitude: item.longitude != null ? Number(item.longitude) : null,
    status: item.status ?? '',
  }))
}

// TourAPI direct calls via /api/tour proxy → https://apis.data.go.kr
// Proxy rule in vite.config.js: '/api/tour' → rewrite to '' (strip prefix)
import axios from 'axios'

const BASE = '/api/tour/B551011/KorService2'

export function getFestivalDateRange() {
  const today = new Date()
  const from = new Date(today)
  from.setMonth(from.getMonth() - 1)
  const to = new Date(today)
  to.setMonth(to.getMonth() + 1)
  const fmt = (d) => d.toISOString().slice(0, 10).replace(/-/g, '')
  return { from: fmt(from), to: fmt(to), today: fmt(today) }
}

const SERVICE_KEY =
  import.meta.env.VITE_KOREA_TOURISM_API_KEY ||
  '73c339a232ae3d694d3f76d524933ae3fda8c253c457a71fa3cf0bfe73609c7c'

function extractHomepageUrl(raw) {
  if (!raw) return ''
  const trimmed = String(raw).trim()
  if (trimmed.startsWith('http')) return trimmed
  const match = trimmed.match(/href=["']([^"']+)["']/i)
  return match ? match[1] : ''
}

export async function fetchFestivals({ areaCode = '' } = {}) {
  const { from, to, today } = getFestivalDateRange()

  const params = {
    serviceKey: SERVICE_KEY,
    numOfRows: 100,
    pageNo: 1,
    MobileOS: 'ETC',
    MobileApp: 'EnjoyTrip',
    _type: 'json',
    arrange: 'A',
    eventStartDate: from,
    eventEndDate: to,
  }
  if (areaCode) params.lDongRegnCd = areaCode

  const { data } = await axios.get(`${BASE}/searchFestival2`, { params })

  // flat error format
  if (data?.resultCode && data.resultCode !== '0') {
    throw new Error(`[${data.resultCode}] ${data.resultMsg ?? '관광정보 서비스 오류'}`)
  }

  // nested success format
  const header = data?.response?.header
  if (header && header.resultCode !== '0000') {
    throw new Error(`[${header.resultCode}] ${header.resultMsg ?? '관광정보 서비스 오류'}`)
  }

  const raw = data?.response?.body?.items?.item
  if (!raw) return []

  const items = Array.isArray(raw) ? raw : [raw]

  return items
    .filter((item) => {
      const start = String(item.eventstartdate ?? '')
      const end = String(item.eventenddate ?? start)
      return end >= today && start <= to
    })
    .map((item) => ({
      id: String(item.contentid),
      title: String(item.title ?? '').trim(),
      address: [item.addr1, item.addr2].filter(Boolean).join(' ').trim(),
      image: String(item.firstimage ?? ''),
      startDate: String(item.eventstartdate ?? ''),
      endDate: String(item.eventenddate ?? item.eventstartdate ?? ''),
      latitude: Number(item.mapy) || null,
      longitude: Number(item.mapx) || null,
      homepage: extractHomepageUrl(item.homepage),
    }))
}

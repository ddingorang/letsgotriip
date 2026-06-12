import { chromium } from 'playwright-core';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs', 'screenshots');
const BASE = 'http://localhost:5173';
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

// 캡처할 화면 (순서대로). auth: 로그인 필요 여부
const SCREENS = [
  { file: '01_home',         path: '/',              auth: false },
  { file: '02_search',       path: '/search',        auth: false },
  { file: '03_detail',       path: '/detail/129156', auth: false },
  { file: '04_login',        path: '/login',         auth: false },
  { file: '05_signup',       path: '/signup',        auth: false },
  { file: '06_ai_input',     path: '/ai',            auth: true  },
  { file: '07_ai_result',    path: '/ai/result',     auth: true  },
  { file: '08_plan_edit',    path: '/plan',          auth: true  },
  { file: '09_mypage',       path: '/mypage',        auth: true  },
  { file: '10_community',    path: '/community',     auth: false },
  { file: '11_companion',    path: '/companion',     auth: false },
  { file: '12_badges',       path: '/badges',        auth: true  },
  { file: '13_checklist',    path: '/checklist',     auth: true  },
  { file: '14_review',       path: '/review',        auth: false },
];

async function settle(page) {
  // 외부 이미지/지도 무한 대기를 피하려 networkidle 대신 시간 기반 안정화
  await page.waitForTimeout(2800);
  // 미완료 이미지는 끊어서 회색 박스로 (레이아웃 유지)
  await page.evaluate(() => {
    for (const img of [...document.images]) {
      if (!img.complete) { img.removeAttribute('src'); img.removeAttribute('srcset'); }
    }
  }).catch(() => {});
  await page.waitForTimeout(400);
}

const run = async () => {
  const browser = await chromium.launch({ executablePath: EDGE, headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 430, height: 844 },
    deviceScaleFactor: 2,
  });
  const page = await ctx.newPage();
  // 외부 이미지/지도 타일은 빠르게 실패시켜 idle 도달 보장 (visitkorea http, 카카오 타일)
  await ctx.route('**/*', (route) => {
    const url = route.request().url();
    if (/visitkorea\.or\.kr|daumcdn\.net|dapi\.kakao\.com|\.(jpg|jpeg|png|gif|webp)(\?|$)/i.test(url)
        && !url.startsWith(BASE)) {
      return route.abort();
    }
    return route.continue();
  });

  // 로그인 — httpOnly 쿠키 세션 확보
  await page.goto(BASE + '/login', { waitUntil: 'domcontentloaded' });
  const loginRes = await page.evaluate(async () => {
    const r = await fetch('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email: 'test@trip.com', password: 'test1234!' }),
    });
    const d = await r.json().catch(() => ({}));
    return { status: r.status, token: d.accessToken || null };
  });
  console.log('login status:', loginRes.status);

  // 인증 화면은 Pinia 메모리 토큰이 새로고침 시 사라지므로,
  // 앱 부트스트랩(/auth/refresh 쿠키)으로 세션이 복원된다 — 쿠키가 있으면 가드 통과.

  const results = [];
  for (const s of SCREENS) {
    try {
      // AI 결과는 직접 진입 시 최신 추천을 로드하도록 스토어가 처리
      await page.goto(BASE + s.path, { waitUntil: 'domcontentloaded', timeout: 15000 });
      // 가드 리다이렉트 대기 + 데이터 로드
      await settle(page);
      // AI 결과/생성처럼 시간이 더 필요한 경우 추가 대기
      if (s.path === '/ai/result' || s.path === '/plan') await page.waitForTimeout(1500);
      const finalPath = new URL(page.url()).pathname;
      const redirected = finalPath !== s.path && !(s.path === '/' && finalPath === '/');
      await page.screenshot({ path: join(OUT, s.file + '.png'), fullPage: true });
      results.push(`${s.file}: ${redirected ? 'REDIRECT→' + finalPath : 'OK'} `);
    } catch (e) {
      results.push(`${s.file}: FAIL ${e.message.slice(0, 60)}`);
    }
  }
  console.log(results.join('\n'));
  await browser.close();
};

run().catch((e) => { console.error('FATAL', e); process.exit(1); });

// Created: 2026-06-05 16:32:29
import { onUnmounted, watch } from 'vue';

function checkSdk() {
  return new Promise((resolve) => {
    // 이미 로드되어 있다면 바로 반환
    if (window.kakao?.maps?.Map) {
      resolve(true);
      return;
    }
    
    // 로드되지 않았다면 짧게 폴링
    let count = 0;
    const poll = setInterval(() => {
      count++;
      if (window.kakao?.maps?.Map) {
        clearInterval(poll);
        resolve(true);
      } else if (count > 25) { // 5초 대기
        clearInterval(poll);
        console.error('[KakaoMap] SDK가 로드되지 않았습니다. API 키나 도메인 설정을 확인하세요.');
        resolve(false);
      }
    }, 200);
  });
}

export function useKakaoMap(mapEl, { getCenter, onReady } = {}) {
  let kakaoMap = null;
  let markers = [];
  let overlay = null;

  function destroy() {
    if (overlay) overlay.setMap(null);
    markers.forEach(({ marker }) => marker.setMap(null));
    overlay = null;
    markers = [];
    kakaoMap = null;
  }

  function setMarkers(items, { getId, getLat, getLng, onClickItem } = {}) {
    if (!kakaoMap || !window.kakao?.maps) return;
    markers.forEach(({ marker }) => marker.setMap(null));
    markers = [];
    
    items.forEach((item) => {
      const lat = getLat(item);
      const lng = getLng(item);
      if (!lat || !lng) return;
      const pos = new window.kakao.maps.LatLng(lat, lng);
      const marker = new window.kakao.maps.Marker({ position: pos, map: kakaoMap });
      window.kakao.maps.event.addListener(marker, 'click', () => {
        onClickItem?.(item);
        kakaoMap.panTo(pos);
      });
      markers.push({ marker, id: getId(item) });
    });
  }

  function setOverlay(title, lat, lng) {
    if (overlay) overlay.setMap(null);
    overlay = null;
    if (!lat || !lng || !kakaoMap || !window.kakao?.maps) return;
    overlay = new window.kakao.maps.CustomOverlay({
      position: new window.kakao.maps.LatLng(lat, lng),
      content: `<div class="map-marker-label">${title}</div>`,
      yAnchor: 2.6,
      map: kakaoMap,
    });
  }

  function panTo(lat, lng) {
    if (!kakaoMap || !lat || !lng || !window.kakao?.maps) return;
    kakaoMap.panTo(new window.kakao.maps.LatLng(lat, lng));
  }

  watch(() => mapEl.value, async (el) => {
    if (!el) {
      destroy();
      return;
    }

    const ok = await checkSdk();
    if (!ok || !el.isConnected) return;

    if (!kakaoMap) {
      const c = getCenter?.();
      const center = c
        ? new window.kakao.maps.LatLng(c.lat, c.lng)
        : new window.kakao.maps.LatLng(36.5, 127.5);

      try {
        kakaoMap = new window.kakao.maps.Map(el, { center, level: 10 });
        onReady?.(kakaoMap);
      } catch (err) {
        console.error('[KakaoMap] 지도 생성 실패:', err);
      }
    }
  }, { immediate: true });

  onUnmounted(destroy);

  return { setMarkers, setOverlay, panTo, destroy };
}

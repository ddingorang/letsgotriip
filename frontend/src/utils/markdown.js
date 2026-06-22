// Created: 2026-06-22
//
// 의존성 없는 경량 마크다운 → HTML 렌더러(어시스턴트 메시지 전용).
//
// [보안] 입력은 신뢰할 수 없는 LLM 출력이므로 반드시 HTML escape 를 "먼저" 한 뒤
// 마크다운 패턴을 HTML 로 치환한다(XSS 방지). 따라서 사용자/모델이 넣은 원시 <script> 등은
// 텍스트로만 노출되고, 우리가 생성하는 태그(<strong>, <a> 등)만 실제 HTML 이 된다.
//
// 지원: 굵게(**), 기울임(*/_), 제목(#~######), 순서/비순서 목록, 인라인코드(`),
//       코드블록(```), 링크([텍스트](url), target=_blank rel=noopener), 줄바꿈.
// 스트리밍 중(미완성 마크다운)에도 깨지지 않도록 라인 단위로 보수적으로 처리한다.

/** HTML 특수문자 escape — 변환 전 항상 먼저 적용한다. */
function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 안전한 http(s)/mailto 링크인지 검사. javascript: 등 위험 스킴을 차단한다.
 * (escape 이후라 따옴표는 이미 무해하지만, 스킴 화이트리스트로 한 번 더 방어한다.)
 */
function isSafeUrl(url) {
  return /^(https?:\/\/|mailto:|\/)/i.test(url.trim())
}

/** 인라인 변환: 코드 → 링크 → 굵게 → 기울임. escape 된 문자열을 입력으로 받는다. */
function renderInline(text) {
  let out = text

  // 인라인 코드 `code` — 내부는 추가 변환하지 않도록 먼저 placeholder 로 보호하는 대신,
  // 코드 내용은 이미 escape 돼 있으므로 그대로 <code>로 감싼다.
  out = out.replace(/`([^`]+?)`/g, (_, code) => `<code class="md-code">${code}</code>`)

  // 링크 [텍스트](url) — url 은 escape 로 인해 따옴표가 없고, 스킴 화이트리스트로 검증.
  out = out.replace(/\[([^\]]+?)\]\(([^)\s]+?)\)/g, (m, label, url) => {
    // escape 과정에서 &amp; 등이 됐을 수 있으므로 안전성 판단용으로만 복원해 검사.
    const raw = url.replace(/&amp;/g, '&')
    if (!isSafeUrl(raw)) return m // 위험 링크는 변환하지 않고 원문 텍스트로 둔다
    return `<a href="${url}" target="_blank" rel="noopener noreferrer">${label}</a>`
  })

  // 굵게 **text** (기울임보다 먼저)
  out = out.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')

  // 기울임 *text* 또는 _text_
  out = out.replace(/(^|[^*])\*([^*\n]+?)\*(?!\*)/g, '$1<em>$2</em>')
  out = out.replace(/(^|[^\w_])_([^_\n]+?)_(?!\w)/g, '$1<em>$2</em>')

  return out
}

/**
 * 마크다운 문자열을 안전한 HTML 로 변환한다.
 * @param {string} src 어시스턴트 원문(마크다운)
 * @returns {string} v-html 에 바인딩할 HTML
 */
export function renderMarkdown(src) {
  if (src == null) return ''
  const escaped = escapeHtml(src)
  const lines = escaped.split('\n')

  const html = []
  let inCode = false // 코드블록(```) 내부 여부
  let codeBuf = []
  let listType = null // 'ul' | 'ol' | null — 현재 열려 있는 목록

  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = null
    }
  }

  for (const line of lines) {
    const fence = line.match(/^\s*```(.*)$/)
    if (fence) {
      if (inCode) {
        // 코드블록 종료
        html.push(`<pre class="md-pre"><code>${codeBuf.join('\n')}</code></pre>`)
        codeBuf = []
        inCode = false
      } else {
        // 코드블록 시작 — 목록이 열려 있으면 닫는다
        closeList()
        inCode = true
      }
      continue
    }
    if (inCode) {
      codeBuf.push(line)
      continue
    }

    // 제목 (## 제목)
    const heading = line.match(/^\s*(#{1,6})\s+(.*)$/)
    if (heading) {
      closeList()
      const level = heading[1].length
      html.push(`<h${level} class="md-h">${renderInline(heading[2])}</h${level}>`)
      continue
    }

    // 순서 목록 (1. 항목)
    const ol = line.match(/^\s*\d+\.\s+(.*)$/)
    if (ol) {
      if (listType !== 'ol') {
        closeList()
        html.push('<ol class="md-ol">')
        listType = 'ol'
      }
      html.push(`<li>${renderInline(ol[1])}</li>`)
      continue
    }

    // 비순서 목록 (- 항목 / * 항목)
    const ul = line.match(/^\s*[-*]\s+(.*)$/)
    if (ul) {
      if (listType !== 'ul') {
        closeList()
        html.push('<ul class="md-ul">')
        listType = 'ul'
      }
      html.push(`<li>${renderInline(ul[1])}</li>`)
      continue
    }

    // 빈 줄 — 목록을 닫고 단락 구분
    if (line.trim() === '') {
      closeList()
      html.push('')
      continue
    }

    // 일반 텍스트 줄
    closeList()
    html.push(`<span class="md-line">${renderInline(line)}</span>`)
  }

  // 스트리밍 중 미완성 코드블록/목록 정리
  if (inCode) {
    html.push(`<pre class="md-pre"><code>${codeBuf.join('\n')}</code></pre>`)
  }
  closeList()

  // 일반 텍스트 줄 사이는 <br> 로 줄바꿈을 유지(목록/제목/코드 블록 사이는 빈 문자열로 분리).
  return html
    .map((h, i) => {
      if (h === '') return ''
      const isLine = h.startsWith('<span class="md-line"')
      const prev = html[i - 1]
      // 연속된 일반 텍스트 줄 사이에만 <br> 삽입
      if (isLine && prev && prev.startsWith('<span class="md-line"')) {
        return '<br>' + h
      }
      return h
    })
    .join('')
}

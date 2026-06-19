// Created: 2026-06-19
//
// 의존성 없이 브라우저 native WebSocket 위에 동작하는 최소 STOMP 1.2 클라이언트.
// CONNECT / SUBSCRIBE / SEND / MESSAGE / RECEIPT / ERROR 프레임만 다룬다.
//
// BE(WebSocketBrokerConfig) 규약:
//   - raw WebSocket 엔드포인트: /ws  (SockJS 미사용)
//   - 앱(서버 핸들러) destination prefix: /pub   (예: /pub/chat.message.{roomId})
//   - 브로커 broadcast prefix: /topic              (예: /topic/chat.room.{roomId})
//   - 인증: CONNECT 프레임의 Authorization: Bearer <accessToken> 헤더
//
// 사용 예:
//   const client = new StompClient({ url, token })
//   await client.connect()
//   const sub = client.subscribe('/topic/chat.room.1', (msg) => { ... })
//   client.send('/pub/chat.message.1', body, { 'content-type': 'application/json' })
//   client.disconnect()

const NULL = String.fromCharCode(0) // STOMP 프레임 종료 바이트()
const LF = String.fromCharCode(10)  // 줄바꿈(\n)

// STOMP 헤더 값 이스케이프 (STOMP 1.2 규약)
function escapeHeader(v) {
  return String(v)
    .replace(/\\/g, '\\\\')
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/:/g, '\\c')
}

function unescapeHeader(v) {
  return String(v)
    .replace(/\\r/g, '\r')
    .replace(/\\n/g, '\n')
    .replace(/\\c/g, ':')
    .replace(/\\\\/g, '\\')
}

// command + headers(+ body) → STOMP wire 프레임
function buildFrame(command, headers = {}, body = '') {
  let frame = command + LF
  for (const [k, v] of Object.entries(headers)) {
    if (v == null) continue
    frame += `${escapeHeader(k)}:${escapeHeader(v)}` + LF
  }
  frame += LF
  frame += body
  frame += NULL
  return frame
}

// wire 프레임 문자열 → { command, headers, body }
function parseFrame(data) {
  // 끝의 NULL 제거
  const cleaned = data.endsWith(NULL) ? data.slice(0, -1) : data
  const headerEnd = cleaned.indexOf(LF + LF)
  const head = headerEnd === -1 ? cleaned : cleaned.slice(0, headerEnd)
  const body = headerEnd === -1 ? '' : cleaned.slice(headerEnd + 2)

  const lines = head.split(LF)
  const command = lines.shift()
  const headers = {}
  for (const line of lines) {
    const idx = line.indexOf(':')
    if (idx === -1) continue
    const key = unescapeHeader(line.slice(0, idx))
    const value = unescapeHeader(line.slice(idx + 1))
    // 동일 헤더 중복 시 첫 값을 유지(STOMP 규약)
    if (!(key in headers)) headers[key] = value
  }
  return { command, headers, body }
}

export class StompClient {
  /**
   * @param {object} opts
   * @param {string} opts.url        - WebSocket URL (예: ws://host/ws)
   * @param {string} [opts.token]    - accessToken (Authorization 헤더로 전달)
   * @param {function} [opts.onError]
   * @param {function} [opts.onClose]
   */
  constructor({ url, token = null, onError = null, onClose = null } = {}) {
    this.url = url
    this.token = token
    this.onError = onError
    this.onClose = onClose

    this.ws = null
    this.connected = false
    this._subId = 0
    this._receiptId = 0
    this._subs = new Map() // id → { destination, handler }
    this._connectResolve = null
    this._connectReject = null
  }

  connect() {
    return new Promise((resolve, reject) => {
      this._connectResolve = resolve
      this._connectReject = reject

      try {
        this.ws = new WebSocket(this.url)
      } catch (e) {
        reject(e)
        return
      }

      this.ws.onopen = () => {
        const headers = {
          'accept-version': '1.2',
          'heart-beat': '0,0',
          host: '/',
        }
        if (this.token) headers['Authorization'] = `Bearer ${this.token}`
        this._raw(buildFrame('CONNECT', headers))
      }

      this.ws.onmessage = (evt) => this._onMessage(evt.data)

      this.ws.onerror = (evt) => {
        if (this._connectReject) {
          this._connectReject(new Error('WebSocket 연결 오류'))
          this._connectReject = null
          this._connectResolve = null
        }
        if (this.onError) this.onError(evt)
      }

      this.ws.onclose = (evt) => {
        this.connected = false
        if (this._connectReject) {
          this._connectReject(new Error('WebSocket 연결 종료'))
          this._connectReject = null
          this._connectResolve = null
        }
        if (this.onClose) this.onClose(evt)
      }
    })
  }

  _onMessage(data) {
    // 하트비트(빈 LF) 무시
    if (data === LF || data === '') return
    const frame = parseFrame(data)

    switch (frame.command) {
      case 'CONNECTED':
        this.connected = true
        if (this._connectResolve) {
          this._connectResolve(this)
          this._connectResolve = null
          this._connectReject = null
        }
        break
      case 'MESSAGE': {
        const subId = frame.headers['subscription']
        const sub = this._subs.get(subId)
        if (sub && typeof sub.handler === 'function') {
          sub.handler(frame)
        }
        break
      }
      case 'ERROR':
        if (this._connectReject) {
          this._connectReject(new Error(frame.headers.message || 'STOMP ERROR'))
          this._connectReject = null
          this._connectResolve = null
        }
        if (this.onError) this.onError(frame)
        break
      case 'RECEIPT':
      default:
        break
    }
  }

  _raw(str) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(str)
    }
  }

  /**
   * @param {string} destination
   * @param {function} handler - (frame) => void. frame.body 가 메시지 페이로드(문자열)
   * @returns {{ id: string, unsubscribe: function }}
   */
  subscribe(destination, handler) {
    const id = `sub-${this._subId++}`
    this._subs.set(id, { destination, handler })
    this._raw(buildFrame('SUBSCRIBE', { id, destination, ack: 'auto' }))
    return {
      id,
      unsubscribe: () => this.unsubscribe(id),
    }
  }

  unsubscribe(id) {
    if (!this._subs.has(id)) return
    this._subs.delete(id)
    this._raw(buildFrame('UNSUBSCRIBE', { id }))
  }

  /**
   * @param {string} destination - 예: /pub/chat.message.1
   * @param {string|object} body - 객체면 JSON 직렬화
   * @param {object} [headers]
   */
  send(destination, body, headers = {}) {
    const payload = typeof body === 'string' ? body : JSON.stringify(body)
    const merged = {
      destination,
      'content-type': 'application/json',
      'content-length': new TextEncoder().encode(payload).length,
      ...headers,
    }
    this._raw(buildFrame('SEND', merged, payload))
  }

  disconnect() {
    if (!this.ws) return
    try {
      if (this.connected) {
        const receipt = `r-${this._receiptId++}`
        this._raw(buildFrame('DISCONNECT', { receipt }))
      }
    } catch {
      // ignore
    } finally {
      try {
        this.ws.close()
      } catch {
        // ignore
      }
      this.ws = null
      this.connected = false
      this._subs.clear()
    }
  }
}

// 개발 환경에서 BE(Spring) WebSocket이 떠 있는 기본 포트.
// vite dev proxy는 /ws 를 프록시하지 않으므로(ws:true 미설정) 5173에서는 BE로 직접 붙어야 한다.
const DEV_BACKEND_WS_PORT = 9090

/**
 * accessToken을 받아 STOMP 클라이언트를 생성하는 헬퍼.
 * http(s) → ws(s)로 스킴 변환.
 *
 * 우선순위:
 *   1) 명시적 baseUrl(또는 import.meta.env.VITE_API_BASE_URL) 이 있으면 그 origin 사용
 *   2) 없고 현재 페이지가 vite dev(5173)이면 BE dev 포트(9090)로 직접 연결
 *   3) 그 외에는 현재 페이지 origin 사용(운영 동일 출처/리버스 프록시 가정)
 *
 * @param {object} opts
 * @param {string} [opts.token]
 * @param {string} [opts.baseUrl]  - import.meta.env.VITE_API_BASE_URL 등
 * @param {string} [opts.endpoint] - 기본 '/ws'
 */
export function createStompClient({ token = null, baseUrl = '', endpoint = '/ws', ...rest } = {}) {
  let origin = baseUrl

  if (!origin && typeof window !== 'undefined') {
    const loc = window.location
    if (loc.port === '5173') {
      // dev: BE WebSocket으로 직접 연결 (프록시 우회)
      origin = `${loc.protocol}//${loc.hostname}:${DEV_BACKEND_WS_PORT}`
    } else {
      origin = loc.origin
    }
  }

  const wsUrl = origin.replace(/^http/i, 'ws').replace(/\/$/, '') + endpoint
  return new StompClient({ url: wsUrl, token, ...rest })
}

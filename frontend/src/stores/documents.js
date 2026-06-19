// Created: 2026-06-19
//
// 문서(업로드/RAG 인덱싱) 상태 store.
// - 목록:   GET    /api/documents
// - 업로드: POST   /api/documents (multipart 'file')
// - 삭제:   DELETE /api/documents/{id}
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { documentApi } from '@/api/index.js'

export const useDocumentsStore = defineStore('documents', () => {
  const items = ref([])
  const loading = ref(false)
  const uploading = ref(false)
  const error = ref(null)

  // BE 문서 응답 → 화면 표시용으로 정규화(필드명 변형 흡수)
  function normalize(doc) {
    return {
      id: doc.id,
      filename: doc.filename ?? doc.name ?? doc.originalFilename ?? '문서',
      type: doc.type ?? doc.contentType ?? doc.mimeType ?? '',
      status: doc.status ?? 'READY',
      createdAt: doc.createdAt ?? doc.uploadedAt ?? null,
      ...doc,
    }
  }

  async function list() {
    loading.value = true
    error.value = null
    try {
      const { data } = await documentApi.list()
      const raw = Array.isArray(data) ? data : (data?.content ?? [])
      items.value = raw.map(normalize)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '문서 목록을 불러오지 못했어요.'
    } finally {
      loading.value = false
    }
  }

  async function upload(file) {
    uploading.value = true
    error.value = null
    try {
      const formData = new FormData()
      formData.append('file', file)
      const { data } = await documentApi.upload(formData)
      const doc = normalize(data)
      // 최신 업로드가 위로 — 색인 실패(FAILED) 문서도 목록에는 보존(BE가 레코드는 남김).
      items.value.unshift(doc)
      // BE는 색인 실패 시에도 201 + status:FAILED 로 응답한다(예외를 삼킴).
      // 이를 '추가됨' 성공으로 위장하지 않고 실패로 노출한다 — 색인되지 않은 문서는 질문에 쓸 수 없다.
      if (doc.status === 'FAILED') {
        const err = new Error('문서 색인에 실패했어요. 이 문서로는 질문할 수 없어요.')
        err.indexingFailed = true
        error.value = err.message
        throw err
      }
      return doc
    } catch (e) {
      // 색인 실패(위에서 throw)는 이미 error.value 설정됨 — 메시지 덮어쓰지 않는다.
      if (!e?.indexingFailed) {
        error.value = e.response?.data?.message ?? e.message ?? '업로드에 실패했어요.'
      }
      throw e
    } finally {
      uploading.value = false
    }
  }

  async function remove(id) {
    error.value = null
    try {
      await documentApi.remove(id)
      // 성공 시에만 로컬 목록에서 제거
      items.value = items.value.filter((d) => d.id !== id)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '삭제에 실패했어요.'
      throw e
    }
  }

  return {
    items,
    loading,
    uploading,
    error,
    list,
    upload,
    remove,
  }
})

// Created: 2026-06-16 13:23:20
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { communityApi, favoriteApi } from '@/api/index.js'

const CATEGORY_LABELS = {
  REVIEW: '후기',
  QUESTION: '질문',
  TIP: '꿀팁',
  RESTAURANT: '맛집',
  COMPANION: '동행',
}

/**
 * Normalize a post from the BE list endpoint (PostSummaryResponse) or
 * detail endpoint (PostResponse) into the shape the FE templates expect.
 * Original fields are preserved; extra fields are added alongside them.
 */
function normalizePost(post) {
  if (!post) return post
  return {
    ...post,
    author: {
      nickname: post.authorNickname ?? post.author?.nickname ?? null,
      avatarUrl: post.authorProfileImageUrl ?? post.author?.avatarUrl ?? null,
    },
    imageUrl: post.imageUrls?.[0] ?? post.thumbnailUrl ?? post.imageUrl ?? null,
    categoryLabel: CATEGORY_LABELS[post.category] ?? post.category ?? null,
  }
}

/**
 * Normalize a comment from the BE (CommentResponse) into the shape
 * templates expect: flat author fields → nested author object.
 */
function normalizeComment(comment) {
  if (!comment) return comment
  return {
    ...comment,
    author: {
      nickname: comment.authorNickname ?? comment.author?.nickname ?? null,
      avatarUrl: comment.authorProfileImageUrl ?? comment.author?.avatarUrl ?? null,
    },
  }
}

export const usePostsStore = defineStore('posts', () => {
  const posts = ref([])
  const currentPost = ref(null)
  const comments = ref([])
  const hasMore = ref(true)
  const loading = ref(false)
  const cursor = ref(null)

  async function fetchPosts(reset = false) {
    if (loading.value) return
    if (reset) {
      posts.value = []
      cursor.value = null
      hasMore.value = true
    }
    if (!hasMore.value) return
    loading.value = true
    try {
      const res = await communityApi.getPosts({ cursor: cursor.value, size: 10 })
      const data = res.data
      const raw = data.content || []
      posts.value.push(...raw.map(normalizePost))
      cursor.value = data.nextCursor || null
      hasMore.value = !!data.nextCursor
    } catch {
      // use mock data when API is not available
      if (reset) posts.value = mockPosts
      hasMore.value = false
    } finally {
      loading.value = false
    }
  }

  async function fetchPost(id) {
    try {
      const res = await communityApi.getPost(id)
      currentPost.value = normalizePost(res.data)
    } catch {
      currentPost.value = mockPosts.find((p) => p.id === Number(id)) || mockPosts[0]
    }
  }

  async function fetchComments(postId) {
    try {
      const res = await communityApi.getComments(postId)
      // BE returns Spring Page({ content: [...] })
      const raw = res.data?.content ?? res.data ?? []
      comments.value = raw.map(normalizeComment)
    } catch {
      comments.value = mockComments
    }
  }

  async function likePost(id) {
    const res = await communityApi.likePost(id)
    const isLiked = res.data
    const delta = isLiked ? 1 : -1
    const post = posts.value.find((p) => p.id === id)
    if (post) { post.likedByMe = isLiked; post.likeCount += delta }
    if (currentPost.value?.id === id) {
      currentPost.value.likedByMe = isLiked
      currentPost.value.likeCount += delta
    }
    return isLiked
  }

  // 댓글 좋아요 토글 — BE POST /comments/{id}/likes → Boolean(좋아요 상태)
  async function likeComment(postId, commentId) {
    const res = await communityApi.likeComment(postId, commentId)
    const isLiked = res.data
    const delta = isLiked ? 1 : -1
    const comment = comments.value.find((c) => c.id === commentId)
    if (comment) {
      comment.likedByMe = isLiked
      comment.likeCount = Math.max(0, (comment.likeCount ?? 0) + delta)
    }
    return isLiked
  }

  // 게시글 북마크(찜) 토글 — BE POST /api/favorites { targetType:'POST', targetId } → Boolean(찜 상태)
  async function bookmarkPost(id) {
    const res = await favoriteApi.toggle('POST', id)
    const isBookmarked = res.data
    const post = posts.value.find((p) => p.id === id)
    if (post) post.bookmarked = isBookmarked
    if (currentPost.value?.id === id) currentPost.value.bookmarked = isBookmarked
    return isBookmarked
  }

  async function deleteComment(postId, commentId) {
    await communityApi.deleteComment(postId, commentId)
    comments.value = comments.value.filter((c) => c.id !== commentId)
    if (currentPost.value?.id === Number(postId)) currentPost.value.commentCount = Math.max(0, (currentPost.value.commentCount ?? 1) - 1)
  }

  async function addComment(postId, content) {
    try {
      await communityApi.createComment(postId, { content })
      await fetchComments(postId)
    } catch {
      comments.value.unshift({
        id: Date.now(),
        author: { nickname: '나', avatarUrl: null },
        content,
        likeCount: 0,
        createdAt: new Date().toISOString(),
      })
    }
  }

  async function updatePost(id, payload) {
    try {
      const res = await communityApi.updatePost(id, payload)
      const updated = normalizePost(res.data)
      currentPost.value = updated
      const idx = posts.value.findIndex((p) => p.id === Number(id))
      if (idx !== -1) posts.value[idx] = updated
    } catch (err) {
      throw err
    }
  }

  async function deletePost(id) {
    try {
      await communityApi.deletePost(id)
      posts.value = posts.value.filter((p) => p.id !== Number(id))
      if (currentPost.value?.id === Number(id)) currentPost.value = null
    } catch (err) {
      throw err
    }
  }

  return { posts, currentPost, comments, hasMore, loading, fetchPosts, fetchPost, fetchComments, likePost, likeComment, bookmarkPost, addComment, deleteComment, updatePost, deletePost }
})

const mockPosts = [
  {
    id: 1,
    title: '성산일출봉 새벽 일출 다녀왔어요',
    content:
      '새벽 5시에 올라간 보람이 있었어요. 정상까지는 20분 정도 걸리는데, 계단이 가팔라서 물 꼭 챙기세요. 정상에서 본 일출이 정말 인생샷이었습니다 🌅 주차는 일찍 가야 자리가 있어요. 4시 반쯤 도착하니 1층 공영주차장에 여유 있었습니다.',
    imageUrl: null,
    category: 'REVIEW',
    categoryLabel: '후기',
    tags: ['#성산일출봉', '#제주여행', '#일출명소'],
    author: { id: 1, nickname: '여행가_지민', avatarUrl: null },
    location: '제주 성산일출봉',
    likeCount: 124,
    commentCount: 12,
    bookmarked: false,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 2,
    title: '부산 감천문화마을 골목골목 탐방기',
    content: '색깔별로 칠해진 계단식 마을이 너무 예뻤어요. 오전 일찍 가면 사진 찍기 좋습니다. 마을 고양이들도 귀여워요 🐱',
    imageUrl: null,
    category: 'REVIEW',
    categoryLabel: '후기',
    tags: ['#감천문화마을', '#부산여행', '#골목여행'],
    author: { id: 2, nickname: '부산러버', avatarUrl: null },
    location: '부산 사하구',
    likeCount: 89,
    commentCount: 7,
    bookmarked: false,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 3,
    title: '경주 불국사 단풍 드디어 절정!',
    content: '이번 주가 딱 절정이에요. 다보탑 앞 단풍이 정말 환상적이었습니다. 주차는 불국사 주차장 말고 건너편 공영주차장 이용하면 훨씬 저렴해요.',
    imageUrl: null,
    category: 'TIP',
    categoryLabel: '꿀팁',
    tags: ['#불국사', '#경주', '#단풍'],
    author: { id: 3, nickname: '역사덕후', avatarUrl: null },
    location: '경주 불국사',
    likeCount: 203,
    commentCount: 24,
    bookmarked: true,
    createdAt: new Date(Date.now() - 172800000).toISOString(),
  },
  {
    id: 4,
    title: '제주 협재해수욕장 에메랄드 바다 🌊',
    content: '물이 너무 맑아서 스노클링 하기 딱 좋아요. 오전에는 한적하고 오후에는 비치 카페들이 활기찹니다. 일몰도 엄청 예뻐요!',
    imageUrl: null,
    category: 'REVIEW',
    categoryLabel: '후기',
    tags: ['#협재해수욕장', '#제주', '#스노클링'],
    author: { id: 4, nickname: '바다사랑', avatarUrl: null },
    location: '제주 한림읍',
    likeCount: 156,
    commentCount: 19,
    bookmarked: false,
    createdAt: new Date(Date.now() - 259200000).toISOString(),
  },
]

const mockComments = [
  {
    id: 1,
    author: { nickname: '제주러버', avatarUrl: null },
    content: '우와 일출 진짜 예쁘네요! 저도 다음 주에 가요 🙌',
    likeCount: 8,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 2,
    author: { nickname: '한라산지기', avatarUrl: null },
    content: '주차 꿀팁 감사합니다. 새벽 4시 반쯤 도착하면 될까요?',
    likeCount: 2,
    createdAt: new Date(Date.now() - 2520000).toISOString(),
  },
  {
    id: 3,
    author: { nickname: '오름마스터', avatarUrl: null },
    content: '혼자 가셨나요? 저도 혼자 여행 계획 중인데 괜찮을까요?',
    likeCount: 1,
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  },
]

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

const BOARD_EXCLUDED_CATEGORIES = new Set(['RESTAURANT'])

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

function isBoardListPost(post) {
  return !BOARD_EXCLUDED_CATEGORIES.has(post?.category)
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
  // 현재 적용된 카테고리 필터(enum). 전체일 때 null → 무필터.
  const category = ref(null)
  // 댓글 페이지네이션 상태 — 누적(append) 방식
  const commentsPage = ref(0)
  const hasMoreComments = ref(true)
  // 정직화: 조회 실패를 가짜 데이터로 가리지 않고 error 상태로 노출한다.
  const postsError = ref(null)
  const postError = ref(null)
  const commentsError = ref(null)
  const commentError = ref(null)

  // category(enum) 인자: reset 시에만 적용된다(서버 카테고리 필터 재조회용).
  async function fetchPosts(reset = false, categoryArg) {
    if (loading.value) return
    if (reset) {
      posts.value = []
      cursor.value = null
      hasMore.value = true
      postsError.value = null
      // reset 호출에서 명시된 카테고리로 갱신(undefined면 무필터로 간주)
      category.value = categoryArg ?? null
    }
    if (!hasMore.value) return
    loading.value = true
    try {
      const params = { cursor: cursor.value, size: 10 }
      if (category.value) params.category = category.value
      const res = await communityApi.getPosts(params)
      const data = res.data
      const raw = data.content || []
      posts.value.push(...raw.map(normalizePost).filter(isBoardListPost))
      cursor.value = data.nextCursor || null
      hasMore.value = !!data.nextCursor
    } catch (err) {
      // 실패 시 가짜 데이터로 대체하지 않고 에러 상태를 노출한다.
      postsError.value = err
      hasMore.value = false
    } finally {
      loading.value = false
    }
  }

  async function fetchPost(id) {
    postError.value = null
    try {
      const res = await communityApi.getPost(id)
      currentPost.value = normalizePost(res.data)
    } catch (err) {
      currentPost.value = null
      postError.value = err
    }
  }

  // 댓글 조회 — reset(첫 페이지)이면 0페이지부터, 아니면 다음 페이지를 누적(append)한다.
  async function fetchComments(postId, reset = true) {
    commentsError.value = null
    if (reset) {
      comments.value = []
      commentsPage.value = 0
      hasMoreComments.value = true
    }
    if (!hasMoreComments.value) return
    const page = commentsPage.value
    try {
      const res = await communityApi.getComments(postId, { page, size: 10 })
      const data = res.data
      // BE returns Spring Page({ content: [...], last })
      const raw = data?.content ?? data ?? []
      comments.value.push(...raw.map(normalizeComment))
      commentsPage.value = page + 1
      // Spring Page: last=true면 마지막 페이지. content 부재(배열 응답) 시 더 없음으로 간주.
      hasMoreComments.value = data?.content ? data.last === false : false
    } catch (err) {
      if (reset) comments.value = []
      commentsError.value = err
    }
  }

  // 다음 댓글 페이지 로드('더보기')
  async function fetchMoreComments(postId) {
    await fetchComments(postId, false)
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

  // 게시글 북마크(찜) 토글 — BE POST /api/favorites { targetType:'POST', targetId } → { favorited: boolean }
  async function bookmarkPost(id) {
    const post = posts.value.find((p) => p.id === id) ?? (currentPost.value?.id === id ? currentPost.value : null)
    const res = await favoriteApi.toggle('POST', id, post?.title)
    // 응답은 { favorited } 객체다. 객체를 boolean처럼 쓰면 항상 truthy가 되므로 언래핑한다.
    const isBookmarked = res.data.favorited
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
    commentError.value = null
    try {
      await communityApi.createComment(postId, { content })
      await fetchComments(postId)
    } catch (err) {
      // 서버 저장 실패 시 가짜 댓글을 끼워넣지 않고 에러를 노출/전파한다.
      commentError.value = err
      throw err
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

  return { posts, currentPost, comments, hasMore, loading, category, hasMoreComments, postsError, postError, commentsError, commentError, fetchPosts, fetchPost, fetchComments, fetchMoreComments, likePost, likeComment, bookmarkPost, addComment, deleteComment, updatePost, deletePost }
})

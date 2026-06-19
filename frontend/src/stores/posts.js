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
  // 정직화: 조회 실패를 가짜 데이터로 가리지 않고 error 상태로 노출한다.
  const postsError = ref(null)
  const postError = ref(null)
  const commentsError = ref(null)
  const commentError = ref(null)

  async function fetchPosts(reset = false) {
    if (loading.value) return
    if (reset) {
      posts.value = []
      cursor.value = null
      hasMore.value = true
      postsError.value = null
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

  async function fetchComments(postId) {
    commentsError.value = null
    try {
      const res = await communityApi.getComments(postId)
      // BE returns Spring Page({ content: [...] })
      const raw = res.data?.content ?? res.data ?? []
      comments.value = raw.map(normalizeComment)
    } catch (err) {
      comments.value = []
      commentsError.value = err
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

  return { posts, currentPost, comments, hasMore, loading, postsError, postError, commentsError, commentError, fetchPosts, fetchPost, fetchComments, likePost, likeComment, bookmarkPost, addComment, deleteComment, updatePost, deletePost }
})

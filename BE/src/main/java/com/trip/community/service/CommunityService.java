// Created: 2026-06-15 22:19:31
package com.trip.community.service;

import com.trip.community.dto.*;
import com.trip.community.entity.*;
import com.trip.community.entity.enums.PostCategory;
import com.trip.community.repository.*;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ─── 게시글 ───────────────────────────────────────────────

    public CursorPageResponse<PostSummaryResponse> getPosts(PostCategory category, Long cursor, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size + 1);

        List<Post> posts = (category != null)
                ? (cursor == null
                        ? postRepository.findAllByDeletedFalseAndCategoryOrderByIdDesc(category, pageable)
                        : postRepository.findAllByDeletedFalseAndCategoryAndIdLessThanOrderByIdDesc(category, cursor, pageable))
                : (cursor == null
                        ? postRepository.findAllByDeletedFalseOrderByIdDesc(pageable)
                        : postRepository.findAllByDeletedFalseAndIdLessThanOrderByIdDesc(cursor, pageable));

        boolean hasNext = posts.size() > size;
        List<Post> content = hasNext ? posts.subList(0, size) : posts;

        List<PostSummaryResponse> responses = content.stream()
                .map(post -> {
                    int commentCount = commentRepository.countByPostIdAndDeletedFalse(post.getId());
                    String thumbnailUrl = postImageRepository.findFirstByPostIdOrderByDisplayOrderAsc(post.getId())
                            .map(PostImage::getImageUrl).orElse(null);
                    return PostSummaryResponse.of(post, commentCount, thumbnailUrl);
                }).toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
        return new CursorPageResponse<>(responses, nextCursor, hasNext);
    }

    @Transactional
    public PostResponse getPost(Long postId, Long userId) {
        Post post = findActivePost(postId);
        post.incrementViewCount();

        int commentCount = commentRepository.countByPostIdAndDeletedFalse(postId);
        List<String> imageUrls = getImageUrls(postId);
        boolean likedByMe = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostResponse.of(post, commentCount, imageUrls, likedByMe);
    }

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        User author = findUser(userId);

        Post post = postRepository.save(Post.builder()
                .author(author)
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .build());

        List<String> imageUrls = saveImages(post, request.imageUrls());

        return PostResponse.of(post, 0, imageUrls, false);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        Post post = findActivePost(postId);
        verifyAuthor(post.getAuthor().getId(), userId);

        post.update(request.title(), request.content(), request.category());

        List<String> imageUrls;
        if (request.imageUrls() != null) {
            replaceImages(post, request.imageUrls());
            imageUrls = request.imageUrls();
        } else {
            imageUrls = getImageUrls(postId);
        }

        int commentCount = commentRepository.countByPostIdAndDeletedFalse(postId);
        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return PostResponse.of(post, commentCount, imageUrls, likedByMe);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findActivePost(postId);
        verifyAuthor(post.getAuthor().getId(), userId);

        // 이미지 파일 삭제
        postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId)
                .forEach(img -> fileStorageService.delete(img.getImageUrl()));
        postImageRepository.deleteAllByPostId(postId);

        post.delete();
    }

    // ─── 좋아요 ───────────────────────────────────────────────

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = findActivePost(postId);
        User user = findUser(userId);

        boolean liked = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(like -> {
                    postLikeRepository.delete(like);
                    post.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    postLikeRepository.save(PostLike.builder().post(post).user(user).build());
                    post.incrementLikeCount();
                    return true;
                });

        // 새로 좋아요가 켜졌고 본인 글이 아닐 때 글 작성자에게 알림 (커밋 후 적재)
        if (liked && !post.getAuthor().getId().equals(userId)) {
            eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                    post.getAuthor().getId(),
                    "community",
                    "내 글을 좋아해요",
                    user.getNickname() + "님이 ‘" + post.getTitle() + "’을 좋아해요.",
                    "/community/" + post.getId()));
        }

        return liked;
    }

    // ─── 댓글 ────────────────────────────────────────────────

    public Page<CommentResponse> getComments(Long postId, Long userId, Pageable pageable) {
        findActivePost(postId);
        return commentRepository.findAllByPostIdAndDeletedFalse(postId, pageable)
                .map(comment -> {
                    boolean likedByMe = userId != null && commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId);
                    return CommentResponse.of(comment, likedByMe);
                });
    }

    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        Comment comment = findActiveComment(commentId);
        User user = findUser(userId);

        boolean liked = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .map(like -> {
                    commentLikeRepository.delete(like);
                    comment.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    commentLikeRepository.save(CommentLike.builder().comment(comment).user(user).build());
                    comment.incrementLikeCount();
                    return true;
                });

        // 새로 좋아요가 켜졌고 본인 댓글이 아닐 때 댓글 작성자에게 알림 (커밋 후 적재)
        if (liked && !comment.getAuthor().getId().equals(userId)) {
            eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                    comment.getAuthor().getId(),
                    "community",
                    "내 댓글을 좋아해요",
                    user.getNickname() + "님이 회원님의 댓글을 좋아해요.",
                    "/community/" + comment.getPost().getId()));
        }

        return liked;
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
        Post post = findActivePost(postId);
        User author = findUser(userId);

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(request.content())
                .build();
        Comment saved = commentRepository.save(comment);

        // 게시글 작성자에게 알림 (본인 댓글 제외, 커밋 후 적재)
        if (!post.getAuthor().getId().equals(userId)) {
            eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                    post.getAuthor().getId(),
                    "community",
                    "내 글에 댓글이 달렸어요",
                    "‘" + post.getTitle() + "’에 " + author.getNickname() + "님이 댓글을 남겼어요.",
                    "/community/" + post.getId()));
        }

        return CommentResponse.of(saved, false);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentCreateRequest request) {
        Comment comment = findActiveComment(commentId);
        verifyAuthor(comment.getAuthor().getId(), userId);
        comment.update(request.content());
        boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        return CommentResponse.of(comment, likedByMe);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findActiveComment(commentId);
        verifyAuthor(comment.getAuthor().getId(), userId);
        comment.delete();
    }

    // ─── 이미지 헬퍼 ─────────────────────────────────────────

    private List<String> saveImages(Post post, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return Collections.emptyList();

        for (int i = 0; i < imageUrls.size(); i++) {
            postImageRepository.save(PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build());
        }
        return imageUrls;
    }

    private void replaceImages(Post post, List<String> newUrls) {
        postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId())
                .forEach(img -> fileStorageService.delete(img.getImageUrl()));
        postImageRepository.deleteAllByPostId(post.getId());
        saveImages(post, newUrls);
    }

    private List<String> getImageUrls(Long postId) {
        return postImageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId)
                .stream().map(PostImage::getImageUrl).toList();
    }

    // ─── 내부 헬퍼 ───────────────────────────────────────────

    private Post findActivePost(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new GeneralException(ResponseCode.POST_NOT_FOUND));
    }

    private Comment findActiveComment(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new GeneralException(ResponseCode.COMMENT_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
    }

    private void verifyAuthor(Long authorId, Long requestUserId) {
        if (!authorId.equals(requestUserId)) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
    }
}

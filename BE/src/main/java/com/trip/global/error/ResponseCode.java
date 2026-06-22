package com.trip.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * API 응답 코드 관리 enum
 *
 * 모든 에러 코드를 한 곳에서 중앙 관리
 * 새로운 에러 추가 시 이 파일에만 추가하면 됨
 *
 * 구성요소:
 * - httpStatus: 실제 HTTP 상태코드 (404, 500 등)
 * - code: 클라이언트가 에러를 식별하는 문자열 ("USER404")
 * - message: 사람이 읽을 수 있는 에러 메시지
 */
@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // 정상 code
    OK(HttpStatus.OK, "200", "Ok"),

    // Common Error
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "요청 값을 확인해주세요."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다. 로그인 후 이용해주세요."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),
    _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "허용되지 않는 요청 방식입니다."),

    // User Error
    USER_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "USER401", "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER409", "이미 존재하는 사용자입니다."),

    // JWT Error
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401", "유효하지 않은 토큰입니다."),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4011", "만료된 토큰입니다."),
    JWT_UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4012", "지원하지 않는 토큰입니다."),
    JWT_MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4013", "잘못된 형식의 토큰입니다."),
    JWT_MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "JWT4014", "토큰이 존재하지 않습니다."),

    // Redis Error
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "SESSION401", "세션이 존재하지 않습니다."),
    SESSION_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "SESSION4011", "토큰 재사용이 감지되어 세션이 만료되었습니다."),

    // Attraction Error
    ATTRACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ATTR404", "존재하지 않는 관광지입니다."),
    INVALID_KEYWORD(HttpStatus.BAD_REQUEST, "ATTR400", "검색어는 2자 이상이어야 합니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "ATTR502", "외부 API 호출에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // Plan Error
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN404", "존재하지 않는 여행 계획입니다."),
    PLAN_FORBIDDEN(HttpStatus.FORBIDDEN, "PLAN403", "해당 여행 계획에 접근 권한이 없습니다."),
    PLAN_PERIOD_CONFLICT(HttpStatus.CONFLICT, "PLAN4091", "기간 축소로 인해 장소가 있는 일자가 삭제됩니다. 해당 일자의 장소를 먼저 삭제해주세요."),
    PLAN_VERSION_CONFLICT(HttpStatus.CONFLICT, "PLAN4092", "여행 계획이 다른 세션에서 수정되었습니다. 최신 버전을 다시 조회해주세요."),
    DUPLICATE_PLACE(HttpStatus.CONFLICT, "PLAN4093", "해당 일자에 이미 추가된 장소입니다."),
    INVALID_PLAN_PERIOD(HttpStatus.BAD_REQUEST, "PLAN400", "여행 기간이 유효하지 않습니다. 시작일은 종료일보다 이전이어야 하며 최대 14일입니다."),
    PLAN_COMPARE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "PLAN4001", "비교할 계획을 2개 이상 선택해주세요."),

    // Recommend Error
    RECO_NOT_FOUND(HttpStatus.NOT_FOUND, "RECO404", "존재하지 않는 추천 결과입니다."),
    RECO_FORBIDDEN(HttpStatus.FORBIDDEN, "RECO403", "해당 추천 결과에 접근 권한이 없습니다."),
    RECO_IN_PROGRESS(HttpStatus.CONFLICT, "RECO409", "이미 추천 요청이 진행 중입니다. 잠시 후 다시 시도해주세요."),
    RECO_EMPTY_RESULT(HttpStatus.UNPROCESSABLE_ENTITY, "RECO422", "AI가 유효한 일정을 생성하지 못했습니다. 다른 조건으로 다시 시도해주세요."),
    AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "RECO502", "AI 추천 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // Community Error
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404", "존재하지 않는 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT404", "존재하지 않는 댓글입니다."),
    HOTPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "HOTPLACE404", "존재하지 않는 핫플입니다."),

    // Notice Error
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE404", "존재하지 않는 공지입니다."),

    // Notification Error
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI404", "존재하지 않는 알림입니다."),

    // File Error
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE500", "파일 업로드에 실패했습니다."),

    // Album Error
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALBUM404", "존재하지 않는 앨범입니다."),

    // Companion Error
    COMPANION_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANION404", "존재하지 않는 동행 게시글입니다."),
    COMPANION_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANION_APP404", "존재하지 않는 신청입니다."),
    COMPANION_ALREADY_APPLIED(HttpStatus.CONFLICT, "COMPANION409", "이미 신청한 동행입니다."),
    COMPANION_POST_CLOSED(HttpStatus.BAD_REQUEST, "COMPANION400", "모집이 마감된 동행입니다."),
    COMPANION_SELF_APPLY(HttpStatus.BAD_REQUEST, "COMPANION4001", "본인 게시글에는 신청할 수 없습니다."),
    COMPANION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "COMPANION4092", "이미 처리된 신청입니다."),
    COMPANION_FULL(HttpStatus.CONFLICT, "COMPANION4093", "정원이 마감되어 더 이상 승인할 수 없습니다."),
    COMPANION_APPROVED_CANCEL(HttpStatus.CONFLICT, "COMPANION4094", "이미 승인된 신청은 취소할 수 없어요."),
    COMPANION_DUPLICATE_APPLY(HttpStatus.CONFLICT, "COMPANION4095", "이미 신청이 접수되었어요. 잠시 후 다시 확인해주세요."),
    COMPANION_CAPACITY_REDUCED_BELOW_CURRENT(HttpStatus.CONFLICT, "COMPANION4096", "현재 참여 인원보다 적은 인원으로는 정원을 줄일 수 없어요.");

    private final HttpStatus httpStatus;
    private final String code; // 클라이언트 식별용 코드 (예: "USER404"
    private final String message; // 기본 에러 메시지

    /**
     * 예외와 함께 메시지를 반환
     * 기본 메시지에 예외 메시지를 붙여서 반환
     * 예시: "존재하지 않는 사용자입니다. - Could not find user id 1"
     */
    public String getMessage(Throwable e) {

        return this.getMessage(this.message + " - " + e.getMessage());
    }

    /**
     * 메시지가 null이거나 blank면 기본 메시지(this.message)로 fallback
     * 예시: getMessage("") → "존재하지 않는 사용자입니다."
     *       getMessage("커스텀 메시지") → "커스텀 메시지"
     */
    public String getMessage(String message) {

        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }
}

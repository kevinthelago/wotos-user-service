package com.wotos.wotosuserservice.model;

/**
 * Standard error envelope returned by the service: {@code {"error": {"code", "message"}}}.
 */
public class ErrorResponse {

    private final ErrorDetail error;

    public ErrorResponse(String code, String message) {
        this.error = new ErrorDetail(code, message);
    }

    public ErrorDetail getError() {
        return error;
    }

    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

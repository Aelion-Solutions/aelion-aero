package com.aelion.aero.common.api;

/**
 * Non-success HTTP response from the panel.
 */
public final class PanelApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public PanelApiException(int statusCode, String responseBody) {
        super("Panel API error HTTP " + statusCode + (responseBody == null || responseBody.isBlank()
                ? ""
                : ": " + truncate(responseBody)));
        this.statusCode = statusCode;
        this.responseBody = responseBody == null ? "" : responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    private static String truncate(String body) {
        return body.length() <= 200 ? body : body.substring(0, 200) + "...";
    }
}

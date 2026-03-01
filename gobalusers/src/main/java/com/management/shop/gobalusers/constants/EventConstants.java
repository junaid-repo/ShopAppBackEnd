package com.management.shop.gobalusers.constants;

public enum EventConstants {

    USER_REG("USER_REG"),
    OTP_SENT("OTP_SENT"),
    OTP_VERIFIED("OTP_VERIFIED"),
    USER_LOGGED_IN("USER_LOGGED_IN"),
    USER_LOGGED_OUT("USER_LOGGED_OUT"),
    PASSWORD_RESET_REQUESTED("PASSWORD_RESET_REQUESTED"),
    PASSWORD_RESET_COMPLETED("PASSWORD_RESET_COMPLETED");

    private final String eventName;

    EventConstants(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}

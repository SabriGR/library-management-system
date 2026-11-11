package com.sabrigur.entity;

public enum ReservationStatus {
    /** The member waits for a copy. */
    WAITING,
    /** A copy was returned and the member was notified. */
    READY,
    /** The member borrowed the book. */
    FULFILLED,
    CANCELLED
}

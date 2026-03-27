package com.rs.doanmonhoc.dto;

import java.time.Instant;

/** Một lượt vào–ra trong ngày (checkOut null = đang trong ca). */
public record AttendanceSegmentDto(Instant checkIn, Instant checkOut) {}

package kz.spt.lib.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class RateQueryDto {

    public Long parkingId;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm", timezone = JsonFormat.DEFAULT_TIMEZONE)
    public Date inDate;

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm", timezone = JsonFormat.DEFAULT_TIMEZONE)
    public Date outDate;

    public Boolean cashlessPayment;

    public Long dimensionId;
}

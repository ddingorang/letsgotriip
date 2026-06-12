package com.trip.global.error.exception.handler;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class RecommendHandler extends GeneralException {

    public RecommendHandler(ResponseCode errorCode) {
        super(errorCode);
    }
}

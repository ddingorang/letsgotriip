package com.trip.global.error.exception.handler;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class RedisHandler extends GeneralException {

    public RedisHandler(ResponseCode errorCode) {
        super(errorCode);
    }
}

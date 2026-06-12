package com.trip.global.error.exception.handler;


import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class JwtHandler extends GeneralException {

    public JwtHandler(ResponseCode errorCode) {
        super(errorCode);
    }
}

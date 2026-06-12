package com.trip.global.error.exception.handler;


import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class UserHandler extends GeneralException {

    public UserHandler(ResponseCode errorCode) { super(errorCode); }
}

package com.trip.global.error.exception.handler;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class AttractionHandler extends GeneralException {

    public AttractionHandler(ResponseCode errorCode) { super(errorCode); }
}

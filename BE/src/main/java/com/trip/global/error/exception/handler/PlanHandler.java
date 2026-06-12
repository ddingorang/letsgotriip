package com.trip.global.error.exception.handler;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;

public class PlanHandler extends GeneralException {

    public PlanHandler(ResponseCode errorCode) { super(errorCode); }
}

package com.example.demo5.dto.call;

import com.example.demo5.entity.CallLog;
import lombok.Getter;

@Getter
public class CreateCallResponse {
    private final Long callLogId;
    private final String status;

    public CreateCallResponse(CallLog callLog) {
        this.callLogId = callLog.getCallLogId();
        this.status = callLog.getStatus().name();
    }
}

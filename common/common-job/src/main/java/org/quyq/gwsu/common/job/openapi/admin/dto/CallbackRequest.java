package org.quyq.gwsu.common.job.openapi.admin.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 回调请求DTO
 */
public class CallbackRequest implements Serializable {
    private static final long serialVersionUID = 42L;

    private List<CallbackData> callbackList;

    public CallbackRequest() {
    }

    public CallbackRequest(List<CallbackData> callbackList) {
        this.callbackList = callbackList;
    }

    public List<CallbackData> getCallbackList() {
        return callbackList;
    }

    public void setCallbackList(List<CallbackData> callbackList) {
        this.callbackList = callbackList;
    }

    @Override
    public String toString() {
        return "CallbackRequest{" +
                "callbackList=" + callbackList +
                '}';
    }
}

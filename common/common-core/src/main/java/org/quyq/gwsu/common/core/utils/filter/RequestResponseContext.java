package org.quyq.gwsu.common.core.utils.filter;


import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description
 */
@Data
public class RequestResponseContext {

    private final String path;
    private final String method;
    private final MultiValueMap<String, String> headers;
    private final MultiValueMap<String, String> queryParams;
    private final Map<String, Object> attributes = new HashMap<>();

    private int httpStatus = 200;
    private final MultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<>();

    //原始响应体内容
    private String originalResponseBody;
    //如果有修改，修改后的响应体内容
    private Object modifiedResponseBody;


    public RequestResponseContext(String path, String method,
                                  MultiValueMap<String, String> headers,
                                  MultiValueMap<String, String> queryParams) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.queryParams = queryParams;
    }

    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    public void addResponseHeader(String name, String value) {
        responseHeaders.add(name, value);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }


}

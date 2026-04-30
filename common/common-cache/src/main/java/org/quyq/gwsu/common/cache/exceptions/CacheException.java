package org.quyq.gwsu.common.cache.exceptions;


import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/4/17
 * @description
 */
public class CacheException extends BasicException {
    public CacheException(ReturnCode code) {
        super(code);
    }

    public CacheException(ReturnCode code, String message) {
        super(code, message);
    }

    public CacheException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public CacheException(String message) {
        super(message);
    }

    public CacheException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "CACHE";
    }
}

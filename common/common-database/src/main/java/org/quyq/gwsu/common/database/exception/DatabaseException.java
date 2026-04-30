package org.quyq.gwsu.common.database.exception;


import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.BasicException;

/**
 * @author Quyq
 * @date 2026/3/24
 * @description
 */
public class DatabaseException extends BasicException {

    public DatabaseException(ReturnCode code) {
        super(code);
    }

    public DatabaseException(ReturnCode code, String message) {
        super(code, message);
    }

    public DatabaseException(ReturnCode code, Throwable cause) {
        super(code, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }

    @Override
    protected String errorType() {
        return "DB";
    }
}

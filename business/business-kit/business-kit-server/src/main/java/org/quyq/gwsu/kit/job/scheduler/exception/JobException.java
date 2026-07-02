package org.quyq.gwsu.kit.job.scheduler.exception;

/**
 * 任务异常
 */
public class JobException extends RuntimeException {

    public JobException() {
    }

    public JobException(String message) {
        super(message);
    }

}

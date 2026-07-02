package org.quyq.gwsu.common.job.context;

/**
 * 任务执行上下文
 */
public class XxlJobContext {

    /**
     * 执行成功
     */
    public static final int HANDLE_CODE_SUCCESS = 200;

    /**
     * 执行失败
     */
    public static final int HANDLE_CODE_FAIL = 500;

    /**
     * 执行超时
     */
    public static final int HANDLE_CODE_TIMEOUT = 502;


    // ---------------------- 基础信息 ----------------------

    /**
     * 任务ID
     */
    private final long jobId;

    /**
     * 任务参数
     */
    private final String jobParam;


    // ---------------------- 日志信息 ----------------------

    /**
     * 日志ID
     */
    private final long logId;

    /**
     * 日志时间戳
     */
    private final long logDateTime;

    /**
     * 日志文件名
     */
    private final String logFileName;


    // ---------------------- 分片信息 ----------------------

    /**
     * 分片序号
     */
    private final int shardIndex;

    /**
     * 分片总数
     */
    private final int shardTotal;


    // ---------------------- 处理结果 ----------------------

    /**
     * 处理码：200=成功, 500=失败, 502=超时
     */
    private int handleCode;

    /**
     * 处理消息
     */
    private String handleMsg;


    public XxlJobContext(long jobId,
                         String jobParam,
                         long logId,
                         long logDateTime,
                         String logFileName,
                         int shardIndex,
                         int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.logId = logId;
        this.logDateTime = logDateTime;
        this.logFileName = logFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;

        this.handleCode = HANDLE_CODE_SUCCESS;  // 默认成功
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public long getLogId() {
        return logId;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }


    // ---------------------- 工具方法 ----------------------

    /**
     * 上下文存储（支持子线程继承）
     */
    private static final InheritableThreadLocal<XxlJobContext> contextHolder = new InheritableThreadLocal<>();

    /**
     * 设置上下文
     */
    public static void setXxlJobContext(XxlJobContext xxlJobContext) {
        contextHolder.set(xxlJobContext);
    }

    /**
     * 获取上下文
     */
    public static XxlJobContext getXxlJobContext() {
        return contextHolder.get();
    }

}

package org.quyq.gwsu.common.job.handler.impl;

import org.quyq.gwsu.common.job.context.XxlJobContext;
import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.glue.GlueTypeEnum;
import org.quyq.gwsu.common.job.handler.IJobHandler;
import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.quyq.gwsu.common.job.util.ScriptUtil;

import java.io.File;

/**
 * 脚本任务处理器
 */
public class ScriptJobHandler extends IJobHandler {

    private final int jobId;
    private final long glueUpdatetime;
    private final String gluesource;
    private final GlueTypeEnum glueType;

    public ScriptJobHandler(int jobId, long glueUpdatetime, String gluesource, GlueTypeEnum glueType) {
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.glueType = glueType;

        // 清理旧脚本文件
        File glueSrcPath = new File(XxlJobFileAppender.getGlueSrcPath());
        if (glueSrcPath.exists()) {
            File[] glueSrcFileList = glueSrcPath.listFiles();
            if (glueSrcFileList != null) {
                for (File glueSrcFileItem : glueSrcFileList) {
                    if (glueSrcFileItem.getName().startsWith(jobId + "_")) {
                        glueSrcFileItem.delete();
                    }
                }
            }
        }
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {

        // 校验
        if (!glueType.isScript()) {
            XxlJobHelper.handleFail("glueType[" + glueType + "] invalid.");
            return;
        }

        // 命令
        String cmd = glueType.getCmd();

        // 生成脚本文件
        String scriptFileName = XxlJobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(String.valueOf(jobId))
                .concat("_")
                .concat(String.valueOf(glueUpdatetime))
                .concat(glueType.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, gluesource);
        }

        // 日志文件
        String logFileName = XxlJobContext.getXxlJobContext().getLogFileName();

        // 脚本参数：0=param、1=分片序号、2=分片总数
        String jobParam = XxlJobHelper.getJobParam();
        String[] scriptParams = new String[3];
        scriptParams[0] = jobParam != null ? jobParam : "";
        scriptParams[1] = String.valueOf(XxlJobContext.getXxlJobContext().getShardIndex());
        scriptParams[2] = String.valueOf(XxlJobContext.getXxlJobContext().getShardTotal());

        // 执行
        XxlJobHelper.log("----------- script file:" + scriptFileName + " -----------");
        int exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, scriptParams);

        if (exitValue == 0) {
            XxlJobHelper.handleSuccess();
            return;
        } else {
            XxlJobHelper.handleFail("script exit value(" + exitValue + ") is failed");
            return;
        }

    }

}

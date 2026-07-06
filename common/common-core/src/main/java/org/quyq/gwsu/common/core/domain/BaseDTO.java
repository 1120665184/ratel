package org.quyq.gwsu.common.core.domain;


import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/3/16
 * @description
 */
@Data
public abstract class BaseDTO {


    /**
     * 当前记录起始索引
     */
    @TableField(exist = false)
    @Schema(title = "页码")
    private Integer pageNum = 1;

    @TableField(exist = false)
    @Schema(title = "页码偏移量", hidden = true)
    private Integer pageFrom = 0;

    /**
     * 每页显示记录数
     */
    @TableField(exist = false)
    @Schema(title = "每页记录数")
    private Integer pageSize = 10;

    /**
     * 排序列
     */
    @TableField(exist = false)
    @Schema(title = "排序列")
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    @TableField(exist = false)
    @Schema(title = "排序方向 asc,desc")
    private String asc = "asc";

    /**
     * 定时任务执行参数
     */
    @TableField(exist = false)
    @Schema(title = "定时任务执行参数", hidden = true)
    private JobParams jobParams;

    /**
     * 定时任务执行参数
     *
     * @param jobShardTotal    分片总数
     * @param jobShardIndex    当前分片
     * @param xxlJobId         执行的任务ID
     * @param xxlJobInstanceId 本次执行实例ID
     */
    @Schema(title = "定时任务执行参数")
    public record JobParams(
            @Schema(title = "分片总数")
            int jobShardTotal,
            @Schema(title = "当前分片")
            int jobShardIndex,
            @Schema(title = "执行的任务ID")
            String xxlJobId,
            @Schema(title = "本次执行实例ID")
            String xxlJobInstanceId
    ) {}

    @JsonIgnore
    public String getOrderBy() {
        if (StringUtils.hasText(orderByColumn)) {
            return orderByColumn + " " + asc;
        }
        return CharSequenceUtil.EMPTY;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        if (Objects.nonNull(pageSize)) {
            this.pageFrom = (pageNum - 1) * pageSize;
        }
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        if (Objects.nonNull(pageNum)) {
            this.pageFrom = (pageNum - 1) * pageSize;
        }
    }

    public void setOrderByColumn(String orderByColumn) {
        if (StringUtils.hasText(orderByColumn)) {
            orderByColumn = CharSequenceUtil.toUnderlineCase(orderByColumn);
        }
        this.orderByColumn = orderByColumn;
    }

    public void setAsc(String isAsc) {
        if (StringUtils.hasText(isAsc)) {
            if ("ascending".equals(isAsc)) {
                isAsc = "asc";
            } else if ("descending".equals(isAsc)) {
                isAsc = "desc";
            }
            this.asc = isAsc;
        }
    }

}

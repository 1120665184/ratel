package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 任务组（执行器）
 */
@Data
@TableName("kit_job_group")
public class KitJobGroup {

    @TableId(type = IdType.AUTO)
    private int id;

    @TableField("app_name")
    private String appname;

    private String name;

    @TableField("address_type")
    private int addressType;        // 执行器地址类型：0=自动注册、1=手动录入

    @TableField("address_list")
    private String addressList;     // 执行器地址列表，多地址逗号分隔(手动录入)

    @TableField("access_token")
    private String accessToken;

    @TableField("update_time")
    private Date updateTime;

    // registry list
    @TableField(exist = false)
    private List<String> registryList;  // 执行器地址列表(系统注册)

    public List<String> getRegistryList() {
        if (addressList != null && !addressList.trim().isEmpty()) {
            registryList = List.of(addressList.split(","));
        }
        return registryList;
    }

    @Override
    public String toString() {
        return "KitJobGroup{" +
                "id=" + id +
                ", appname='" + appname + '\'' +
                ", name='" + name + '\'' +
                ", addressType=" + addressType +
                ", addressList='" + addressList + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }

}

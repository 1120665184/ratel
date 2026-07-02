package org.quyq.gwsu.kit.job.domain;

import java.util.Date;
import java.util.List;

/**
 * 任务组（执行器）
 */
public class KitJobGroup {

    private int id;
    private String appname;
    private String name;

    private int addressType;        // 执行器地址类型：0=自动注册、1=手动录入
    private String addressList;     // 执行器地址列表，多地址逗号分隔(手动录入)

    private String accessToken;
    private Date updateTime;

    // registry list
    private List<String> registryList;  // 执行器地址列表(系统注册)

    public List<String> getRegistryList() {
        if (addressList != null && !addressList.trim().isEmpty()) {
            registryList = List.of(addressList.split(","));
        }
        return registryList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAddressType() {
        return addressType;
    }

    public void setAddressType(int addressType) {
        this.addressType = addressType;
    }

    public String getAddressList() {
        return addressList;
    }

    public void setAddressList(String addressList) {
        this.addressList = addressList;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
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

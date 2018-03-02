package com.ft.enums;

import java.util.Objects;

/**
 * 用于标识事务处理的状态
 */
public enum SysEventEunm {

    status_new("NEW", "新事件"),
    status_exception("EXCEPTION", "事件处理异常"),
    status_publish("PUBLISH", "待发布"),
    status_published("PUBLISHED", "已发布"),
    status_process("PROCESS", "待处理"),
    status_processed("PROCESSED", "已处理");

    private String statusCode;
    private String statusName;

    SysEventEunm(String statusCode, String statusName) {
        this.statusCode = statusCode;
        this.statusName = statusName;
    }

    /**
     * 通过statusCode获取statusName
     *
     * @param statusCode
     * @return
     */
    public static String getStatusNameByStatusCode(String statusCode) {
        SysEventEunm[] values = SysEventEunm.values();
        for (SysEventEunm value : values) {
            if (Objects.equals(value.statusCode, statusCode)) {
                return value.statusName;
            }
        }
        return null;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}

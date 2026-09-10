package com.navfirst.lmonitor.lib.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 创建：馥溪凝
 * 日期：2022/04/11 21:31
 * 描述：com.navfirst.dmonitor.lib.exceptions
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RtkconvException extends RuntimeException {

    private int code;

    private String msg;

    public RtkconvException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public RtkconvException(Exception e) {
        super(e);
        this.msg = e.getMessage();
    }

}

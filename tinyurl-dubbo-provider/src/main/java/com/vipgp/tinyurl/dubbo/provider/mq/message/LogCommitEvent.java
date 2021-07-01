package com.vipgp.tinyurl.dubbo.provider.mq.message;

import lombok.Data;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/7 23:38
 */
@Data
public class LogCommitEvent {
    private String fileName;
    private Integer offset;

    public LogCommitEvent(){}

    public LogCommitEvent(String fileName, Integer offset){
        this.fileName=fileName;
        this.offset=offset;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append("LogCommitEvent {");
        sb.append(" fileName=").append(fileName);
        sb.append(" ,offset=").append(offset);
        sb.append(" }");

        return sb.toString();
    }
}

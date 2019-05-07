package cn.edu.sicau.pfdistribution.service.kafka.data;

import lombok.Data;

import java.util.Date;

@Data
public class Message {

    private Long id;
    //id

    private String msg;
    //消息

    private Date sendTime;
    //时间戳


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
}

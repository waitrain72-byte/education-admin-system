package com.example.common.enums;

/*枚举值要与前端页面Course的值保持一致*/
public enum SegmentEnum {

    FIRST("第一大节（08:30 ~ 10:10）"),
    SECOND("第二大节（10:30 ~ 12:10）"),
    THIRD("第三大节（14:00 ~ 15:40）"),
    FORTH("第四大节（16:00 ~ 17:40）"),
    FIFTH("第五大节（19:00 ~ 20:40）"),
    ;
    public String segment;
    SegmentEnum(String segment){
        this.segment = segment;
    }

}

package com.biyao.search.bs.server.common.enums;

/**
 * @desc:
 * @author: xiafang
 * @date: 2020/9/23
 */
public enum SexLabelEnum {
    NO_GENDER(-1, "其他"),
    MALE(0, "男"),
    FEMALE(1, "女");
    /**
     * 性别标签码
     */
    private Integer code;
    /**
     * 性别标签描述
     */
    private String desc;

    SexLabelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String getDescByCode(Integer code) {
        for (SexLabelEnum item : SexLabelEnum.values()) {
            if (item.getCode().equals(code)) {
                return item.getDesc();
            }
        }
        return null;
    }
}

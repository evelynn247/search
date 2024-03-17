package com.biyao.search.as.server.enums;

/**
 * @desc:
 * @author: xiafang
 * @date: 2020/7/10
 */
public enum ActivityEnum {
    NO_ACTIVITY(0,"无活动"),
    GROUP_BUY(1,"一起拼"),
    LADDER_GROUP(2,"阶梯团"),
    BUY2_RETURN_ALLOWANCE(3,"买二返一"),
    ALLOWANCE_DEDUCTION(4,"津贴抵扣"),
    RECOMMEND_SEARCH(5,"轮播图落地页搜索"),
    SUPPLIER_SEARCH(6,"商家店铺页搜本店"),
    SUPPLIER_ALL_SEARCH(7,"商家店铺页搜全站");

    private Integer code;
    private String desc;

    ActivityEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public Integer getCode() {
        return code;
    }
}

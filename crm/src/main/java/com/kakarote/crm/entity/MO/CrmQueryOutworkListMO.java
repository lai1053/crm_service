package com.kakarote.crm.entity.MO;

import com.kakarote.core.entity.PageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("考勤打卡列表查询BO")
public class CrmQueryOutworkListMO extends PageEntity {

    @ApiModelProperty("用户ID")
    private long userId;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;
}

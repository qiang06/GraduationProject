package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@TableName("db_account_details")
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetails implements BaseData {
    @TableId
    Integer id;

    int gender;
    String phone;
    String qq;
    String wx;
    @TableField("`desc`")
    String desc;
}

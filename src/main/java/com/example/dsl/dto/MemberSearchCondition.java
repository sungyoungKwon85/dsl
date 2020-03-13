package com.example.dsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {

    // 회원명, 팀명, 나이(크거나같거나, 작거나같거나)

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

}

package com.example.dsl.dto;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QueryProjection 예제를 보여주기 위해 생성함
 */
@Data
@NoArgsConstructor
public class MemberDtoQP {
    private String username;
    private int age;

    // Q file로 생성이 됨!!
    @QueryProjection
    public MemberDtoQP(String username, int age) {
        this.username = username;
        this.age = age;
    }
}

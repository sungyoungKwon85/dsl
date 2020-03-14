package com.example.dsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.example.dsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom
// 얘는 한계점이 많음
, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}

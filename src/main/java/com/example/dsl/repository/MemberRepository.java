package com.example.dsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    List<Member> findByUsername(String username);
}

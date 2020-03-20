package com.example.dsl.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.dsl.dto.MemberSearchCondition;
import com.example.dsl.dto.MemberTeamDto;
import com.example.dsl.entity.Member;
import com.example.dsl.repository.MemberJpaRepository;
import com.example.dsl.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhereParameter(condition);
    }

    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }

    // 도메인 클래스 컨버터 사용 전
    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }

    // 도메인 클래스 컨버터 사용 후
    // spring boot 를 사용하면 기본으로 됨ㅋ
    // 권장하진 않음 (실무에서 이런 단순한 경우가 별로 없음)
    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Member member) {
        return member.getUsername();
    }

    //  페이징과 정렬
    // /members?page=0&size=3&sort=id,desc&sort=username,desc
    @GetMapping("/members")
    public Page<Member> list(Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        return page;
    }
    @RequestMapping(value = "/members_page", method = RequestMethod.GET)
    public Page<Member> list2(@PageableDefault(size = 12, sort = "username",
        direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        return page;
    }
    // 페이징 정보가 둘 이상이면 접두사로 구분
//    public String list(
//        @Qualifier("member") Pageable memberPageable,
//        @Qualifier("order") Pageable orderPageable, ...

}

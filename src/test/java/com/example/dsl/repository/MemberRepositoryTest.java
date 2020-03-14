package com.example.dsl.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.dto.MemberSearchCondition;
import com.example.dsl.dto.MemberTeamDto;
import com.example.dsl.entity.Member;
import com.example.dsl.entity.QMember;
import com.example.dsl.entity.Team;

import static com.example.dsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    @Test
    public void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> findAll = memberRepository.findAll();
        assertThat(findAll).containsExactly(member);

        List<Member> byUsername = memberRepository.findByUsername("member1");
        assertThat(byUsername).containsExactly(member);
    }

    @Test
    public void searchTest() throws Exception {
        initData();

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> result2 = memberRepository.search(condition2);
        assertThat(result2).extracting("username").containsExactly("member3", "member4");
    }


    @Test
    public void searchPageTest() throws Exception {
        initData();

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, PageRequest.of(0, 3));
        assertThat(result.getContent()).extracting("username").containsExactly("member4");
        assertThat(result.getSize()).isEqualTo(3);

        Page<MemberTeamDto> result3 = memberRepository.searchPageComplex(condition, PageRequest.of(0, 3));
        assertThat(result3.getContent()).extracting("username").containsExactly("member4");
        assertThat(result3.getSize()).isEqualTo(3);

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        Page<MemberTeamDto> result2 = memberRepository.searchPageSimple(condition2, PageRequest.of(0, 3));
        assertThat(result2.getContent()).extracting("username").containsExactly("member3", "member4");
        assertThat(result2.getSize()).isEqualTo(3);

        Page<MemberTeamDto> result4 = memberRepository.searchPageComplex(condition2, PageRequest.of(0, 3));
        assertThat(result4.getContent()).extracting("username").containsExactly("member3", "member4");
        assertThat(result4.getSize()).isEqualTo(3);
    }

    private void initData() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    // MemberRepository에 QuerydslPredicateExecutor를 상속 받음
    // 아래와 같은 방식으로 구현이 가능해진다
    // 한계점
    //   조인X (묵시적 조인은 가능하지만 left join이 불가능하다.)
    //   클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
    //   복잡한 실무환경에서 사용하기에는 한계가 명확하다
    @Test
    public void querydslPredicateExecutorTest() {
        initData();
        Iterable<Member> results = memberRepository.findAll(
            member.age.between(20, 40)
                .and(member.username.eq("member1")));

        for (Member m : results) {
            System.out.println("member1: " + m);
        }
    }
}
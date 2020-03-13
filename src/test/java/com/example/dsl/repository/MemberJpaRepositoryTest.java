package com.example.dsl.repository;

import java.util.List;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.dto.MemberSearchCondition;
import com.example.dsl.dto.MemberTeamDto;
import com.example.dsl.entity.Member;
import com.example.dsl.entity.Team;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;


    @Test
    public void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findByID(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> findAll = memberJpaRepository.findAll();
        assertThat(findAll).containsExactly(member);

        List<Member> byUsername = memberJpaRepository.findByUsername("member1");
        assertThat(byUsername).containsExactly(member);
    }

    @Test
    public void basicDslTest() throws Exception {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        List<Member> findAll = memberJpaRepository.findAll_dsl();
        assertThat(findAll).containsExactly(member);

        List<Member> byUsername = memberJpaRepository.findByUsername_dsl("member1");
        assertThat(byUsername).containsExactly(member);
    }

    @Test
    public void searchTest() throws Exception {
        initData();

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result).extracting("username").containsExactly("member4");

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> result2 = memberJpaRepository.searchByBuilder(condition2);
        assertThat(result2).extracting("username").containsExactly("member3", "member4");

        // 이렇게 조건이 없는 경우 모든 쿼리를 불러오는 경우가 생길 수 있으니 항상 페이징을 습관하 할 것
        MemberSearchCondition condition3 = new MemberSearchCondition();
        List<MemberTeamDto> result3 = memberJpaRepository.searchByBuilder(condition3);

    }

    @Test
    public void searchTestWhere() throws Exception {
        initData();

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByWhereParameter(condition);
        assertThat(result).extracting("username").containsExactly("member4");

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> result2 = memberJpaRepository.searchByWhereParameter(condition2);
        assertThat(result2).extracting("username").containsExactly("member3", "member4");
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

}
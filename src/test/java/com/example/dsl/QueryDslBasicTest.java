package com.example.dsl;

import java.util.List;
import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.entity.Member;
import com.example.dsl.entity.QMember;
import com.example.dsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.dsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    // 동시성 문제가 없게끔 설계가 되어 있음
    // 트랜잭션에 바인딩 되도록 분배된다.
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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

    @Test
    public void startJPQL() {
        // 문법 틀리면 runtime에서 잡힘
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl() {
//        QMember m = new QMember("m"); // 같은 테이블을 조인해야 하는 경우 alias가 겹칠 수 있을 때 이걸 씀
//        QMember m = member;
        // import static을 쓰는걸 권장

        // preparedStatement로 된다.
        //  vs Statement
        //  : 가장 큰 차이는 cache 사용여부
        //  : statement는 매번 1. 쿼리 문장 분석, 2. 컴파일, 3. 실행을 반복
        //  : preparedStatement는 1번만 1~3을 하고 cache에 담아서 재사용
        // 문자 더하기가 아님(SQL Injection 공격당함)
        // 문법 틀리면 컴파일 단계에서 잡힘
        Member findMember = queryFactory
            .select(member)
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.eq(10)))
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
            .selectFrom(member)
            // 요렇게 and 를 쓸 때 ,를 이용해도 된다.
            // null 이 들어가면 무시해주므로 동적쿼리 만들때 매우 좋다
            .where(
                member.username.eq("member1"),
                member.age.eq(10)
            )
            .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//            .selectFrom(member)
//            .fetch();
//
//        Member fetchOne = queryFactory
//            .selectFrom(QMember.member)
//            .fetchOne();
//
//        Member fetchFirst = queryFactory
//            .selectFrom(QMember.member)
//            .fetchFirst();

        // 페이징 정보까지
        QueryResults<Member> memberQueryResults = queryFactory
            .selectFrom(member)
            .fetchResults();

        // 얘가 있어서 쿼리가 두방 날라감 (for count)
        memberQueryResults.getTotal();
        List<Member> results = memberQueryResults.getResults();

        // count만 따로 할 수도 있음
        long fetchCount = queryFactory
            .selectFrom(member)
            .fetchCount();
    }

    /**
     * 1. 나이 내림차순 desc
     * 2. 이름 올림차순 asc
     * 2에서 없으면 마지막에 출력
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> fetch = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member memberNull = fetch.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

}

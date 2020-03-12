package com.example.dsl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.entity.Member;
import com.example.dsl.entity.QMember;
import com.example.dsl.entity.QTeam;
import com.example.dsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.example.dsl.entity.QTeam.*;
import static com.querydsl.jpa.JPAExpressions.*;
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

    @Test
    public void paging1() {
        List<Member> fetch = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); // 요거땜에 count 쿼리가 한번 더 나감
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        // data type이 여러개 올때 Tuple을 쓰는데 실제로는 DTO를 많이씀
        Tuple result = queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
            )
            .from(member)
            .fetchOne();
        assertThat(result.get(member.count())).isEqualTo(4);
        assertThat(result.get(member.age.sum())).isEqualTo(100);
        assertThat(result.get(member.age.avg())).isEqualTo(25);
        assertThat(result.get(member.age.max())).isEqualTo(40);
        assertThat(result.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     */
    @Test
    public void group() throws Exception {
        List<Tuple> results = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

        Tuple teamA = results.get(0);
        Tuple teamB = results.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        for (Member m : result) {
            System.out.println("Member: " + m);
        }

        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * (연관관계가 없는 조인)
     * (각 DB가 성능 최적화 하긴 함. 다 다름)
     * 제약?: 외부조인 불가능했었는데.. on을 사용하면 가능
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

        assertThat(result)
            .extracting("username")
            .containsExactly("teamA", "teamB");
    }

    /**
     * join on 은 JPA 2.1부터 지원
     * 조인대상 필터링
     *
     * 회원과 팀을 조인하면서 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team) // left outer join
            .on(team.name.eq("teamA"))
            .fetch();
        // 정말 외부조인이 필요한 경우에만 사용하면 됨

        for (Tuple tu : result) {
            System.out.println("tuple: " + tu);
        }

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get(0).get(team)).isNotNull();
        assertThat(result.get(2).get(team)).isNull();
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // on을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능
        // 문법을 잘 봐야 한다
        // 일반조인: leftJoin(member.team, team)
        // on조인: from(member).leftJoin(team).on(XXX)
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team)
            .on(member.username.eq(team.name))
            .fetch();

        for (Tuple t : result) {
            System.out.println("tuple: " + t);
        }
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetch_join() {
        em.flush();
        em.clear();

        Member member1 = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();

        Member member2 = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();
        boolean loaded2 = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded2).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void sub_query() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                select(memberSub.age.max())
                    .from(memberSub)
            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(40);
    }

    /**
     * 나이가 평균보다 많은 회원 조회
     */
    @Test
    public void sub_queryGoe() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.avg())
                    .from(memberSub)
            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(30, 40);
    }

    @Test
    public void sub_queryIn() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            ))
            .fetch();

        assertThat(result).extracting("age")
            .containsExactly(20, 30, 40);
    }

    @Test
    public void sub_query_select() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
            .select(
                member.username,
                select(memberSub.age.avg())
                    .from(memberSub)
            )
            .from(member)
            .fetch();

        for (Tuple t : result) {
            System.out.println("tuple: " + t);
        }

        // JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리는 지원하지 않음
        // dsl도 마찬가지
        // select절이 되는거는 하이버네이트가 지원해주기 때문임
        // from 절 서브쿼리 해결방안?
        //   서브쿼리를 join으로 변경,
        //   application에서 쿼리를 2번 분리해서 실행
        //   또는 nativeSQL을 사용한다
    }

    @Test
    public void case_query() throws Exception {
        List<String> result = queryFactory
            .select(
                member.age
                    .when(10).then("열살")
                    .when(20).then("스무살")
                    .otherwise("기타")
            )
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("@@@ " + s);
        }
    }

    // 요런건 application에서 하는게 낫다!!
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
            .select(
                new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20")
                    .when(member.age.between(21, 30)).then("21~30")
                    .otherwise("etc")
            )
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("@@@ " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        List<Tuple> a = queryFactory
            .select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();

        for (Tuple s : a) {
            System.out.println("@@@ " + s);
        }
    }

    @Test
    public void concat() throws Exception {
        List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

        for (String s : result) {
            System.out.println("@@@ " + s);
        }
    }

}

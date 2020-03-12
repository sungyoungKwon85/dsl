package com.example.dsl;

import java.util.List;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.dto.MemberDto;
import com.example.dsl.dto.MemberDtoQP;
import com.example.dsl.dto.QMemberDtoQP;
import com.example.dsl.dto.UserDto;
import com.example.dsl.entity.Member;
import com.example.dsl.entity.QMember;
import com.example.dsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;


import static com.example.dsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QueryDslIntermidiateTest {

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
    public void simpleProjection() throws Exception {
        List<String> fetch = queryFactory
            .select(member.username)
            .from(member)
            .fetch();
    }

    @Test
    public void tupleProjection() throws Exception {
        // Tuple은 querydsl package임
        // 의존성 제거를 위해 Repository에서 쓰는 것 괜찮지만 Service에서 쓰기에는 바람직 하지 않은 듯
        // Dto로 변환하는 것을 권장
        List<Tuple> fetch = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();

        for (Tuple t : fetch) {
            System.out.println("username: " + t.get(member.username));
            System.out.println("age: " + t.get(member.age));
        }
    }

    @Test
    public void findDtoByJPQL() throws Exception {
        // 패키지 명 적는거 별로임
        List<MemberDto> resultList = em.createQuery(
            "select new com.example.dsl.dto.MemberDto("
                + "m.username, m.age) "
                + "from Member m", MemberDto.class)
            .getResultList();

        for (MemberDto m : resultList) {
            System.out.println("memberDto: " + m);
        }
    }

    // setter를 활용한 dsl
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (MemberDto m : result) {
            System.out.println("MemberDto: " + m);
        }
    }

    // Field를 활용한 dsl
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
            // getter, setter 무시하고 field에 바로 꽂힘 (리플렉션인듯)
            .select(Projections.fields(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (MemberDto m : result) {
            System.out.println("MemberDto: " + m);
        }
    }

    // constructor 활용한 dsl
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (MemberDto m : result) {
            System.out.println("MemberDto: " + m);
        }
    }

    // 필드 프로젝션에 as를 넣을라믄..
    @Test
    public void as_different_field_name() throws Exception {
        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"),
                member.age))
            .from(member)
            .fetch();

        for (UserDto m : result) {
            System.out.println("UserDto: " + m);
        }
    }

    // 필드 프로젝션에 + 서브쿼리에 as를 넣을라믄..
    @Test
    public void as_select_subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                ExpressionUtils.as(member.username, "name"),

                ExpressionUtils.as(JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub), "age")
            ))
            .from(member)
            .fetch();
        for (UserDto m : result) {
            System.out.println("UserDto: " + m);
        }
    }

    // 생정자 프로젝션은 별달리 할 거 없음
    @Test
    public void as_findDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (UserDto m : result) {
            System.out.println("UserDto: " + m);
        }
    }

    @Test
    public void findDtoByQueryProjection() throws Exception {
        // 생성자에 뭐가 필요한지, 티입이 뭔지 컴파일단계에서 다 맞춰주므로 매우 안정적임
        // 단점은
        // Q file을 생성해야 한다는 것
        // 의존관계.. querydsl에 대한 의존성이 생겨버림
        // API, View 등등 여러군데에서 쓸 텐데 dsl 의존성이 들어 있다보니 architecture 측면에서 아쉬움
        List<MemberDtoQP> result = queryFactory
            .select(new QMemberDtoQP(member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDtoQP m : result) {
            System.out.println("MemberDtoQP: " + m);
        }
    }

    // 동적쿼리 방법1
    @Test
    public void boolean_builder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        // 필수값이면 생성할때 넣어도 됨
//        BooleanBuilder booleanBuilder = new BooleanBuilder(member.username.eq(usernameCond));
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (usernameCond != null) {
            booleanBuilder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            booleanBuilder.and(member.age.eq(ageCond));
        }
        return queryFactory
            .selectFrom(member)
            .where(booleanBuilder)
            .fetch();
    }

    // 동적쿼리 방법2
    // 더 좋은 듯?
    @Test
    public void where_param() throws Exception {
        String usernameParam = null;
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
            .selectFrom(member)
            .where(usernameEq(usernameCond), ageEq(ageCond))
//            .where(allEq(usernameCond, ageCond))
            .fetch();

    }

    // 요렇게 메서드로 빼버렸으니 얼마나 좋음?
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    // 활용 예
    // 광고 상태 isValid + 날짜가 IN -> isServiceable


    // 수정/삭제 배치 쿼리
    @Test
    @Commit
    public void bulk_update() throws Exception {
        //1 member1 = 10 -> DB member1
        //2 member2 = 20 -> DB member2
        //3 member3 = 30 -> DB member3
        //4 member4 = 40 -> DB member4
        long count = queryFactory
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(27))
            .execute();
        //1 member1 = 10 -> DB 비회원
        //2 member2 = 20 -> DB 비회원
        //3 member3 = 30 -> DB member3
        //4 member4 = 40 -> DB member4

        // 주의사항
        // 영속성 컨텍스트에 다 올라가 있음
        // 벌크 연산은 영속성 컨텍스트를 무시하고 DB로 바로 쏨
        // DB와 영속성 컨텍스트와의 상태가 달라져 버림

        // !업데이트 하고 나서 요걸 때려서 영속성 컨텍스트를 날려버려야 한다.
        em.flush();
        em.clear();

        // 영속성 컨텍스를 안날리면?
        //   DB에서 select 해 왔어도 영속성컨텍스트에 데이터가 있으면 그걸 써버림
        //   update 해버린게 무쓸모가 되버림 -> repeatable read(?)
        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();

        for (Member m : result) {
            System.out.println("member1: " + m);
        }
    }

    @Test
    public void bulk_add() throws Exception {
        long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(1))
            .execute();
    }

    @Test
    public void bulk_delete() throws Exception {
        long count = queryFactory
            .delete(member)
            .where(member.age.gt(19))
            .execute();
    }

    @Test
    public void sql_function() throws Exception {
        // JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다
        // H2Dialect.class에 등록되어 있는 것만 가능
        List<String> result = queryFactory
            .select(Expressions.stringTemplate(
                "function('replace', {0}, {1}, {2})",
                member.username, "member", "M"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() throws Exception {
        queryFactory
            .select(member.username)
            .from(member)
            // 이런 일반화된 함수들은 사실 dsl에서 내장하고 있음
            .where(member.username.eq(Expressions.stringTemplate(
                "function('lower', {0})", member.username)
            ))
            .fetch();

        List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(member.username.lower()))
            .fetch();



        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}

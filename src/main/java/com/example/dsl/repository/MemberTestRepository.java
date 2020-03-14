package com.example.dsl.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.example.dsl.dto.MemberSearchCondition;
import com.example.dsl.entity.Member;
import com.example.dsl.entity.QMember;
import com.example.dsl.repository.support.Querydsl4RepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;

import static com.example.dsl.entity.QMember.member;
import static com.example.dsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basciSelect() {
        return select(member)
            .from(member)
            .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
            .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()));
        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();
        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    // searchPageByApplyPage와 동일한 동작을 함
    // 한번 더 추상화 한 거임
    public Page<Member> applyPaginationSimple(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query -> query
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
        );
    }

    public Page<Member> applyPaginationComplex(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
            query -> query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())
                ),
            countQuery -> countQuery
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                    teamNameEq(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
        );
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teanName) {
        return hasText(teanName) ? team.name.eq(teanName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}

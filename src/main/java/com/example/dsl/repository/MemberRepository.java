package com.example.dsl.repository;

import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import com.example.dsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom
// 얘는 한계점이 많음
, QuerydslPredicateExecutor<Member> {

    List<Member> findByUsername(String username);

    Slice<Member> findSliceBy(Pageable pageable);

    @Query(value = "select m from Member m left join m.team t",
    countQuery = "select count(m) from Member m")
    Page<Member> findPageBy(Pageable pageable);

    // 영속성 컨텍스트에만 있는 상태에서 update query를 때려버리면 영속성 컨텍스트와 싱크가 안맞을 수 있음
    // 벌크 연산에서 조심해야 한다 (dsl에서도 마찬가지)
    // 벌크 연산 이후에는 영속성 컨텍스트를 날려버려야 한다
    @Modifying(clearAutomatically = true)
    @Query(value = "update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    @EntityGraph(attributePaths = {"team"})
    List<Member> findAllBy();

    @EntityGraph(attributePaths = {"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);


    // Dialect에 따라 동작 방식이 달라짐
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findLockByUsername(String username);
}

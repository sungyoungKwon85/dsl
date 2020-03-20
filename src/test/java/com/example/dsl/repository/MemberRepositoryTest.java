package com.example.dsl.repository;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.dto.MemberDto;
import com.example.dsl.dto.MemberSearchCondition;
import com.example.dsl.dto.MemberTeamDto;
import com.example.dsl.entity.Member;
import com.example.dsl.entity.Team;

import static com.example.dsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

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

    @Test
    public void sortTest() throws Exception {
        initData();
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Direction.DESC, "username"));
        Page<Member> result = memberRepository.findPageBy(pageRequest);
        List<Member> members = result.getContent();
        // Page는 total count까지 날림
        // join 조건 등이 복잡해지면 count query 성능이 안좋아질 수 있다
        // 최적화 방법은 searchPageComplex를 참고 하자
        // 또는 findPageBy에서 countQuery는 분리
        long totalElements = result.getTotalElements();
        System.out.println("total elements: " + totalElements);

        // !! 항상 Dto로 변환해서 API response를 만들어야 한다
        Page<MemberDto> memberDtos =
            result.map(mm -> new MemberDto(mm.getUsername(), mm.getAge()));
        for (MemberDto m : memberDtos) {
            System.out.println("username: " + m.getUsername());
        }
    }

    @Test
    public void sliceTest() throws Exception {
        initData();
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Direction.DESC, "username"));
        // count query를 안날림
        Slice<Member> result = memberRepository.findSliceBy(pageRequest);
        List<Member> members = result.getContent();
        for (Member m : members) {
            System.out.println("username: " + m.getUsername());
        }
    }

    @Test
    public void bulkTest() throws Exception {
        initData();
        int i = memberRepository.bulkAgePlus(20);
        System.out.println(i);

        // 주의점은 bulkAgePlus 메서드에 코멘트 해둠
        em.flush();
//        em.clear(); // @Modifying 에서 clearAutomatically=true 하면 필요 없음

        List<Member> all = memberRepository.findAll();
        for (Member m : all) {
            System.out.println("Age: " + m.getAge());
        }
    }

    // N+1, N + 1
    @Test
    public void findMemberLazy() throws Exception {
        initData();
        em.flush();
        em.clear();
        //when
        // Member만 DB에서 가져옴
        // Memeber 안에 있는 Team은? null로 할 수는 없으니 프록시를 통해 텅빈 결과를 채움
        List<Member> members = memberRepository.findAll();
        //then
        for (Member member : members) {
            System.out.println("member: " + member.getUsername());

            // hibernateProxy 클래스가 딱 나옴
            // Lazy 여서 가짜 클래스가 나온거임
            System.out.println("member.teamClass: " + member.getTeam().getClass());

            // 이제서야 team을 위한 query를 날림
            // query가 for문만큼 날라감
            // N + 1 문제 발생
            // 해결 -> fetch join
            System.out.println("member.team: " + member.getTeam().getName());
        }

        // fetch join
        List<Member> memberFetchJoin = memberRepository.findMemberFetchJoin();
        System.out.println("===========================");

        // fetch join은 매번 query짜기 귀찮음
        // 그래서 나온게 entityGraph임
        List<Member> myAll = memberRepository.findAllBy();
        // 요렇게 해두됨 ㅋㅋ
        List<Member> memberEntityGraph = memberRepository.findMemberEntityGraph();
    }

    @Test
    public void jpaHintTest() {
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();;
        em.clear();

//        Member found = memberRepository.findById(member1.getId()).get();
//        found.setUsername("member2");

//        em.flush(); // 이순간 dirty checking 되서 update 쿼리가 나간다
        // 단점이 있다.
        // find 하는 순간 메모리에 올려둔다. 원본 보관을 위해.

        // hint에 readonly를 줬기 때문에 update 쿼리가 안날라감
        Member found = memberRepository.findReadOnlyByUsername("member1");
        found.setUsername("member2");
        em.flush();

        // 사실 hint를 쓸일이 크게 없다.
        // 실무에서 readonly 옵션을 다 넣는다 하더라도 엄청난 차이가 나지 않는다..
        // 진짜 트래픽이 높은 쿼리에만 넣어도 된다
    }
}
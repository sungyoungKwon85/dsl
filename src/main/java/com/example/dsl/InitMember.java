package com.example.dsl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.entity.Member;
import com.example.dsl.entity.Team;

import lombok.RequiredArgsConstructor;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
        // 이부분을 왜 바로 안하냐?
        // 스프링 라이프사이클 상, Transactional과 PostConstruct가 겹칠 수가 없다.
        // @PostConstruct는 해당 빈 자체만 생성되었다고 가정하고 호출됨.
        // 해당 빈에 관련된 AOP등을 포함한, 전체 스프링 애플리케이션 컨텍스트가 초기화 된 것을 의미하지 않음
        // 트랜잭션을 처리하는 AOP등은 스프링의 후 처리기(post processer)가 완전히 동작을 끝내서,
        // 스프링 애플리케이션 컨텍스트의 초기화가 완료되어야 적용됨.
        // 즉, @PostConstruct는 해당빈의 AOP 적용을 보장하지 않는다.
        // https://www.inflearn.com/questions/26902
        // https://stackoverflow.com/questions/17346679/transactional-on-postconstruct-method

    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }
}

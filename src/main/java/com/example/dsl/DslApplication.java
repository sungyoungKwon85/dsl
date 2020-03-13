package com.example.dsl;

import javax.persistence.EntityManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.querydsl.jpa.impl.JPAQueryFactory;

@SpringBootApplication
public class DslApplication {

	public static void main(String[] args) {
		SpringApplication.run(DslApplication.class, args);
	}


	// 모든 스레드에서 쓰면 동시성 문제가 없을까?
	// entityManager가 트랜잭션 단위로 분리되서 동작함
	// 중간에 Proxy가 있음
	@Bean
	JPAQueryFactory jpaQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}

}

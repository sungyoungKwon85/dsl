package com.example.dsl;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import com.example.dsl.entity.Hello;
import com.example.dsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Commit
class DslApplicationTests {

	@Test
	void contextLoads() {
	}

}

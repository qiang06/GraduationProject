package com.myproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class MyProjectBackenApplicationTests {

	@Test
	void contextLoads() {

		System.out.println(new BCryptPasswordEncoder().encode("123456"));

	}

}

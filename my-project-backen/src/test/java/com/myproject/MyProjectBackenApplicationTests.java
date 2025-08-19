package com.myproject;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SpringBootTest
class MyProjectBackenApplicationTests {

	@Test
	void contextLoads() throws IOException, TimeoutException {
		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("127.0.0.1"); // 或者实际 IP
		factory.setPort(5672);
		factory.setUsername("guest");
		factory.setPassword("guest");
		
		try (Connection conn = factory.newConnection()) {
			System.out.println("连接成功!");
		}
	}

}

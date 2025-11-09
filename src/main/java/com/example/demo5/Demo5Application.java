package com.example.demo5;

// (1) JdbcTemplate을 임포트합니다.
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// (2) 생성자 주입을 위해 'Autowired'를 임포트합니다.
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class Demo5Application {

    // (3) JdbcTemplate 멤버 변수 선언
    private final JdbcTemplate jdbcTemplate;

    // (4) 생성자를 통해 JdbcTemplate을 '강제로' 주입받도록 합니다.
    //     스프링은 이 코드를 보고, JdbcTemplate을 만들기 위해
    //     DB 커넥션 풀(HikariPool)을 *반드시* 실행하게 됩니다.
    @Autowired
    public Demo5Application(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(Demo5Application.class, args);
    }

}
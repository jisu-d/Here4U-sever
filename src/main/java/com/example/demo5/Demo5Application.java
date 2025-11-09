package com.example.demo5;

// (1) JdbcTemplate을 임포트합니다.
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// (2) 생성자 주입을 위해 'Autowired'를 임포트합니다.
import org.springframework.beans.factory.annotation.Autowired;

// (A) CommandLineRunner와 List를 임포트합니다.
import org.springframework.boot.CommandLineRunner;
import java.util.List;

@SpringBootApplication
// (B) CommandLineRunner 인터페이스를 구현(implements)합니다.
public class Demo5Application implements CommandLineRunner {

    // (3) JdbcTemplate 멤버 변수 선언
    private final JdbcTemplate jdbcTemplate;

    // (4) 생성자를 통해 JdbcTemplate을 '강제로' 주입받도록 합니다.
    @Autowired
    public Demo5Application(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(Demo5Application.class, args);
    }

    // (C) CommandLineRunner의 run 메소드를 구현합니다.
    // 이 메소드는 스프링 부트가 시작된 *직후*에 자동으로 실행됩니다.
    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- [TEST] DB 연결 테스트 및 테이블 목록 조회 시작 ---");

        try {
            // (D) JdbcTemplate을 사용하여 'SHOW TABLES' 쿼리를 실행합니다.
            // DB에 있는 모든 테이블 이름을 List<String> 형태로 가져옵니다.
            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);

            if (tables.isEmpty()) {
                System.out.println("--- [TEST] DB 연결 성공! (테이블이 아직 없습니다) ---");
            } else {
                System.out.println("--- [TEST] DB 연결 성공! 테이블 목록: ---");
                // 찾은 테이블 이름을 하나씩 출력합니다.
                tables.forEach(table -> System.out.println(" - " + table));
            }

        } catch (Exception e) {
            System.err.println("--- [TEST] DB 연결 또는 쿼리 실행 실패 ---");
            e.printStackTrace();
        }

        System.out.println("--- [TEST] DB 연결 테스트 종료 ---");
    }
}
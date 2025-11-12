package com.example.demo5.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @Column(name = "member_id", length = 10) // <-- DB 스키마와 동일하게 길이 지정
    private String memberId; // <-- Long 에서 String으로 변경

    // @GeneratedValue(strategy = GenerationType.IDENTITY) // <-- 이 줄 삭제!

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
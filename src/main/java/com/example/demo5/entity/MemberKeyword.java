package com.example.demo5.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member_keyword")
@Getter
@Setter
@NoArgsConstructor
public class MemberKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long keywordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "keyword", columnDefinition = "json")
    private String keyword; // Stores JSON string like "[]" or ["keyword1", "keyword2"]
}

package com.example.demo5.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "call_log")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_log_id")
    private Long callLogId;

    @Column(name = "call_sid", unique = true)
    private String callSid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "call_data", columnDefinition = "json")
    private String callData;

    @Column(name = "call_result_sentiment")
    private String callResultSentiment;

    @Column(name = "simple_summary", length = 50)
    private String simpleSummary;

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

    public void setCallData(String callData) {
        this.callData = callData;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public void setCallResultSentiment(String callResultSentiment) {
        this.callResultSentiment = callResultSentiment;
    }

    public void setSimpleSummary(String simpleSummary) {
        this.simpleSummary = simpleSummary;
    }

    @Builder
    public CallLog(Member member, CallType callType, CallStatus status) {
        this.member = member;
        this.callType = callType;
        this.status = status;
    }

    public enum CallType {
        MANUAL, AUTO
    }

    public enum CallStatus {
        QUEUED, COMPLETED, FAILED
    }
}

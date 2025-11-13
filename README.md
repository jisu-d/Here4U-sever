# Here4U-sever API 명세서

이 문서는 Here4U-sever의 주요 API 엔드포인트와 데이터 전송 객체(DTO)에 대한 명세서입니다.

---

## 1. 회원 관련 API (`/api/members`)

### 1.1. 회원 추가
*   **엔드포인트**: `POST /api/members`
*   **설명**: 새로운 회원을 시스템에 추가합니다.
*   **요청 바디**: `CreateMemberRequest`
    ```json
    {
      "phoneNumber": "string" // 회원의 전화번호 (필수)
    }
    ```
*   **응답 바디**: `MemberResponse`
    ```json
    {
      "memberId": "string",    // 생성된 회원의 고유 ID
      "phoneNumber": "string"  // 회원의 전화번호
    }
    ```
*   **상태 코드**:
    *   `201 Created`: 회원 생성 성공
    *   `400 Bad Request`: 유효하지 않은 요청 (예: 이미 등록된 전화번호)

### 1.2. 수동 전화 걸기
*   **엔드포인트**: `POST /api/members/{memberId}/calls`
*   **설명**: 특정 회원에게 수동으로 전화를 겁니다.
*   **경로 변수**:
    *   `memberId`: `string` - 전화를 걸 회원의 ID
*   **요청 바디**: `CreateCallRequest`
    ```json
    {
      "scriptId": "string" // 사용할 스크립트 ID (선택 사항)
    }
    ```
*   **응답 바디**: `CreateCallResponse`
    ```json
    {
      "callLogId": long,     // 생성된 통화 기록의 ID
      "callSid": "string",   // Twilio에서 발급한 통화 SID
      "memberId": "string",  // 통화 대상 회원의 ID
      "callType": "MANUAL",  // 통화 유형 (MANUAL)
      "status": "QUEUED"     // 통화 상태 (QUEUED)
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 통화 시작 요청 성공
    *   `404 Not Found`: 해당 ID의 회원을 찾을 수 없음

### 1.3. 자동 전화 스케줄 등록
*   **엔드포인트**: `POST /api/members/{memberId}/schedules`
*   **설명**: 특정 회원에 대한 자동 전화 스케줄을 등록합니다.
*   **경로 변수**:
    *   `memberId`: `string` - 스케줄을 등록할 회원의 ID
*   **요청 바디**: `ScheduleRequest`
    ```json
    {
      "startDate": "YYYY-MM-DD", // 스케줄 시작일
      "frequency": "DAILY" | "WEEKLY" | "MONTHLY", // 통화 주기
      "callTime": "HH:MM:SS",    // 통화 시간
      "isActive": true | false   // 스케줄 활성화 여부 (기본값: true)
    }
    ```
*   **응답 바디**: `CreateScheduleResponse`
    ```json
    {
      "scheduleId": long,        // 생성된 스케줄의 ID
      "memberId": "string",      // 스케줄이 등록된 회원의 ID
      "startDate": "YYYY-MM-DD", // 스케줄 시작일
      "frequency": "DAILY" | "WEEKLY" | "MONTHLY", // 통화 주기
      "callTime": "HH:MM:SS",    // 통화 시간
      "isActive": true | false   // 스케줄 활성화 여부
    }
    ```
*   **상태 코드**:
    *   `201 Created`: 스케줄 등록 성공
    *   `404 Not Found`: 해당 ID의 회원을 찾을 수 없음

### 1.4. 자동 전화 스케줄 변경
*   **엔드포인트**: `PATCH /api/members/{memberId}/schedules/{scheduleId}`
*   **설명**: 특정 회원의 자동 전화 스케줄을 변경합니다.
*   **경로 변수**:
    *   `memberId`: `string` - 스케줄을 변경할 회원의 ID
    *   `scheduleId`: `long` - 변경할 스케줄의 ID
*   **요청 바디**: `ScheduleRequest` (변경할 필드만 포함)
    ```json
    {
      "startDate": "YYYY-MM-DD", // 스케줄 시작일
      "frequency": "DAILY" | "WEEKLY" | "MONTHLY", // 통화 주기
      "callTime": "HH:MM:SS",    // 통화 시간
      "isActive": true | false   // 스케줄 활성화 여부
    }
    ```
*   **응답 바디**: `UpdateScheduleResponse`
    ```json
    {
      "scheduleId": long,        // 변경된 스케줄의 ID
      "memberId": "string",      // 스케줄이 등록된 회원의 ID
      "startDate": "YYYY-MM-DD", // 스케줄 시작일
      "frequency": "DAILY" | "WEEKLY" | "MONTHLY", // 통화 주기
      "callTime": "HH:MM:SS",    // 통화 시간
      "isActive": true | false   // 스케줄 활성화 여부
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 스케줄 변경 성공
    *   `404 Not Found`: 해당 ID의 스케줄을 찾을 수 없음
    *   `403 Forbidden`: 해당 스케줄을 변경할 권한이 없음

### 1.5. 회원 관심 키워드 조회
*   **엔드포인트**: `GET /api/members/{memberId}/keyword`
*   **설명**: 특정 회원의 관심 키워드 목록을 조회합니다.
*   **경로 변수**:
    *   `memberId`: `string` - 키워드를 조회할 회원의 ID
*   **응답 바디**: `MemberKeywordResponse`
    ```json
    {
      "memberKeyword": ["string", "string"] // 회원의 관심 키워드 목록
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 키워드 조회 성공
    *   `404 Not Found`: 해당 ID의 회원 키워드를 찾을 수 없음

### 1.6. 회원 상태 태그 조회
*   **엔드포인트**: `GET /api/members/{memberId}/status`
*   **설명**: 특정 회원의 현재 심리 상태 태그를 조회합니다.
*   **경로 변수**:
    *   `memberId`: `string` - 상태 태그를 조회할 회원의 ID
*   **응답 바디**: `MemberStatusTagResponse`
    ```json
    {
      "memberStatus": "안전" | "주의" | "확인 필요" // 회원의 현재 심리 상태 태그
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 상태 태그 조회 성공
    *   `404 Not Found`: 해당 ID의 회원 상태를 찾을 수 없음




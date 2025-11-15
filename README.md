# Here4U-sever API 명세서

이 문서는 Here4U-sever의 주요 API 엔드포인트와 데이터 전송 객체(DTO)에 대한 명세서입니다.

---

## 1. 회원 관련 API (`/api/members`)

### 1.1. 회원 추가
*   **엔드포인트**: `POST /api/members`
*   **설명**: 새로운 회원을 시스템에 추가합니다.
*   **요청 바디**: `CreateMemberRequest`
    *   `phoneNumber` (string, 필수): 회원의 전화번호
    ```json
    {
      "phoneNumber": "010-1234-5678"
    }
    ```
*   **응답 바디**: `MemberResponse`
    *   `memberId` (string): 생성된 회원의 고유 ID
    *   `phoneNumber` (string): 회원의 전화번호
    ```json
    {
      "memberId": "aB1cD",
      "phoneNumber": "010-1234-5678"
    }
    ```
*   **상태 코드**:
    *   `201 Created`: 회원 생성 성공
    *   `400 Bad Request`: 유효하지 않은 요청 (예: 이미 등록된 전화번호)

### 1.2. 수동 전화 걸기
*   **엔드포인트**: `POST /api/members/{memberId}/calls`
*   **설명**: 특정 회원에게 수동으로 전화를 겁니다.
*   **경로 변수**:
    *   `memberId` (string): 전화를 걸 회원의 ID
*   **요청 바디**: `CreateCallRequest`
    *   `scriptId` (string, 선택): 사용할 스크립트 ID
    ```json
    {
      "scriptId": "default-script"
    }
    ```
*   **응답 바디**: `CreateCallResponse`
    ```json
    {
      "callLogId": 1,
      "callSid": "CAxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      "memberId": "aB1cD",
      "callType": "MANUAL",
      "status": "QUEUED"
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 통화 시작 요청 성공
    *   `404 Not Found`: 해당 ID의 회원을 찾을 수 없음

### 1.3. 자동 전화 스케줄 등록
*   **엔드포인트**: `POST /api/members/{memberId}/schedules`
*   **설명**: 특정 회원에 대한 자동 전화 스케줄을 등록합니다.
    *   `frequency`는 `DAILY`, `WEEKLY`, `MONTHLY` 중 하나의 값을 가집니다.
*   **경로 변수**:
    *   `memberId` (string): 스케줄을 등록할 회원의 ID
*   **요청 바디**: `ScheduleRequest`
    ```json
    {
      "startDate": "2025-11-15",
      "frequency": "WEEKLY",
      "callTime": "19:00:00",
      "isActive": true
    }
    ```
*   **응답 바디**: `CreateScheduleResponse`
    ```json
    {
      "scheduleId": 1,
      "memberId": "aB1cD",
      "startDate": "2025-11-15",
      "frequency": "WEEKLY",
      "callTime": "19:00:00",
      "isActive": true
    }
    ```
*   **상태 코드**:
    *   `201 Created`: 스케줄 등록 성공
    *   `404 Not Found`: 해당 ID의 회원을 찾을 수 없음

### 1.4. 자동 전화 스케줄 변경
*   **엔드포인트**: `PATCH /api/members/{memberId}/schedules/{scheduleId}`
*   **설명**: 특정 회원의 자동 전화 스케줄을 변경합니다. 변경할 필드만 요청에 포함합니다.
    *   `frequency`는 `DAILY`, `WEEKLY`, `MONTHLY` 중 하나의 값을 가집니다.
*   **경로 변수**:
    *   `memberId` (string): 스케줄을 변경할 회원의 ID
    *   `scheduleId` (long): 변경할 스케줄의 ID
*   **요청 바디**: `ScheduleRequest`
    ```json
    {
      "frequency": "MONTHLY",
      "isActive": false
    }
    ```
*   **응답 바디**: `UpdateScheduleResponse`
    ```json
    {
      "scheduleId": 1,
      "memberId": "aB1cD",
      "startDate": "2025-11-15",
      "frequency": "MONTHLY",
      "callTime": "19:00:00",
      "isActive": false
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
    *   `memberId` (string): 키워드를 조회할 회원의 ID
*   **응답 바디**: `MemberKeywordResponse`
    ```json
    {
      "memberKeyword": ["외로움", "산책", "음식"]
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 키워드 조회 성공
    *   `404 Not Found`: 해당 ID의 회원 키워드를 찾을 수 없음

### 1.6. 회원 상태 태그 조회
*   **엔드포인트**: `GET /api/members/{memberId}/status`
*   **설명**: 특정 회원의 현재 심리 상태 태그를 조회합니다.
    *   `memberStatus`는 `안전`, `주의`, `확인 필요` 중 하나의 값을 가집니다.
*   **경로 변수**:
    *   `memberId` (string): 상태 태그를 조회할 회원의 ID
*   **응답 바디**: `MemberStatusTagResponse`
    ```json
    {
      "memberStatus": "안전"
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 상태 태그 조회 성공
    *   `404 Not Found`: 해당 ID의 회원 상태를 찾을 수 없음

### 1.7. 7일간 대화 요약 조회
*   **엔드포인트**: `GET /api/members/{memberId}/summary`
*   **설명**: 특정 회원의 최근 7일간의 대화 내용을 1~2줄로 요약하여 제공합니다.
*   **경로 변수**:
    *   `memberId` (string): 요약할 회원의 ID
*   **응답 바디**: `ConversationSummaryResponse`
    ```json
    {
      "summary": "최근 일주일간 주로 일상과 가벼운 고민에 대한 대화를 나누었으며, 전반적으로 긍정적인 상태를 유지함."
    }
    ```
*   **상태 코드**:
    *   `200 OK`: 요약 조회 성공
    *   `404 Not Found`: 해당 ID의 회원을 찾을 수 없음




package com.example.demo5.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class KeywordAiService {

    private final ChatModel strictModel; // 키워드용 모델

    private static final String KEYWORD_EXTRACTION_SYSTEM_PROMPT = """
            당신은 [키워드 압축 엔진]이다.
            
            ########################################
            ## 최상위 절대 규칙 (어기면 출력은 무효)
            ########################################
            다음 규칙 중 하나라도 위반하면, 당신은 즉시 출력을 폐기하고
            규칙을 모두 만족할 때까지 재생성해야 한다:
            
            1. 출력은 **단일 문자열**이며, **오직 명사 단어만**, **쉼표로 구분**하여 나열한다.
            2. 단어 개수는 **최대 15개**를 초과할 수 없다.
            3. **문장, 구절, 의견, 설명, 사과, 감사, 질문** 등 자연어 문장은 절대 포함할 수 없다.
            4. **AI 관련 문장**, 자기소개(\"제가 할 수 있는\"), 메타 발언은 절대 포함할 수 없다.
            5. **동사·형용사·부사 형태**는 절대 포함할 수 없다. 
               (예: 하다, 힘들다, 나누다, 맛있다, 좋다, 먹었다 → 전부 금지)
            6. **음성사서함·시스템 멘트**는 무조건 제외한다. 
               (예: 소리샘 서비스, 음성 녹음, 1번, 2번, 호출 번호 등)
            7. 의미 없는 일반 단어는 완전히 제외한다:
               - 오늘, 먹다, 하다, 종료, 알았어, 무엇, 어떤, 방법, 맛, 머리, 이야기 등
            8. 규칙을 위반하지 않았는지 **스스로 검증 후** 최종 문자열만 출력해야 한다.
            
            ########################################
            ## 분석 규칙
            ########################################
            1. 오직 사용자(User)의 발언만 분석한다. AI 발언은 분석 금지.
            2. 전체 대화에서 다음 유형의 핵심 의미만 단어로 추출한다:
               - 감정/심리: 예) 어려움, 고민, 피로
               - 관심사/주제: 예) 코딩, 알고리즘, 친구, 관계
               - 음식/취향: 예) 소주, 안주, 피자, 고기, 치즈버거
            3. 문장형 표현을 의미 단어로 압축해야 한다.
               - "코딩이 힘들어" → 코딩, 어려움
               - "친구가 많지 않다" → 친구, 고민
            4. 반복적이거나 강조된 개념을 우선 포함한다.
            5. 모든 단어는 반드시 **핵심 의미**를 가진 명사여야 한다.
            
            ########################################
            ## 출력 형식 (엄격)
            ########################################
            - 예: 코딩, 어려움, 알고리즘, 소주, 안주, 피자, 고기, 친구, 고민, 음식
            - 단일 문자열
            - 쉼표로 구분
            - 명사만, 15개 이하
            - 규칙 위반 시 자동으로 재출력
            
    """;

    public String extractKeywords(String conversation) {
        // Create the messages
        SystemMessage systemMessage = new SystemMessage(KEYWORD_EXTRACTION_SYSTEM_PROMPT);
        UserMessage userMessage = new UserMessage("대화:\n" + conversation);

        // Combine messages into a single list
        List<Message> messageList = new ArrayList<>();
        messageList.add(systemMessage);
        messageList.add(userMessage);

        // Create the prompt with the combined list
        Prompt prompt = new Prompt(messageList);

        // Call the model
        return strictModel.call(prompt).getResult().getOutput().getText();
    }
}
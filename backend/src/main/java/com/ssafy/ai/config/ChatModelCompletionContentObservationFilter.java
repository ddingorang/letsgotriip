package com.ssafy.ai.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;

/**
 * 공홈 제공: https://langfuse.com/integrations/frameworks/spring-ai
 * Spring AI의 ChatModel 관찰(Observation) 과정에서 프롬프트(질문)와 컴플리션(응답) 내용을 추출하여
 * 트레이싱 태그로 추가하는 필터 클래스입니다.
 *
 * 이 필터를 통해 추출된 데이터는 OpenTelemetry를 거쳐 Langfuse와 같은 외부 관측성 도구로 전송되며,
 * 결과적으로 Langfuse 대시보드에서 AI와 주고받은 실제 텍스트 내용을 확인할 수 있게 됩니다.
 */
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

    /**
     * 관찰 컨텍스트를 가공하여 필요한 메타데이터(질문/응답 텍스트)를 추가합니다.
     *
     * @param context
     *            현재 관찰 중인 컨텍스트 (ChatModelObservationContext인 경우에만 작동)
     * @return 가공된 컨텍스트
     */
    @Override
    public Observation.Context map(Observation.Context context) {
        if (!(context instanceof ChatModelObservationContext chatModelObservationContext)) {
            return context;
        }
        var prompts = processPrompts(chatModelObservationContext);
        var completions = processCompletion(chatModelObservationContext);
        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.prompt";
            }

            @Override
            public String getValue() {
                return ObservabilityHelper.concatenateStrings(prompts);
            }
        });
        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.completion";
            }

            @Override
            public String getValue() {
                return ObservabilityHelper.concatenateStrings(completions);
            }
        });
        return chatModelObservationContext;
    }

    private List<String> processPrompts(ChatModelObservationContext chatModelObservationContext) {
        return CollectionUtils.isEmpty((chatModelObservationContext.getRequest()).getInstructions()) ? List.of()
                : (chatModelObservationContext.getRequest()).getInstructions().stream().map(Content::getText).toList();
    }

    private List<String> processCompletion(ChatModelObservationContext context) {
        if (context.getResponse() != null && (context.getResponse()).getResults() != null && !CollectionUtils.isEmpty((context.getResponse()).getResults())) {

            return !StringUtils.hasText((context.getResponse()).getResult().getOutput().getText()) ? List.of()
                    : (context.getResponse()).getResults().stream().filter((generation) -> generation.getOutput() != null && StringUtils.hasText(generation.getOutput().getText()))
                            .map((generation) -> generation.getOutput().getText()).toList();
        } else {
            return List.of();
        }
    }
}

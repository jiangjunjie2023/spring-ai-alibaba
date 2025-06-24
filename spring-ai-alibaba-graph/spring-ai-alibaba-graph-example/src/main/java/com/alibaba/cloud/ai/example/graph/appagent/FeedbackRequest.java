package com.alibaba.cloud.ai.example.graph.appagent;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackRequest(

		/* 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,

		/* 是否接受Planner的计划，true为接受，false为重新生成 */
		@JsonProperty(value = "feedback", defaultValue = "true") Boolean feedBack) {

}
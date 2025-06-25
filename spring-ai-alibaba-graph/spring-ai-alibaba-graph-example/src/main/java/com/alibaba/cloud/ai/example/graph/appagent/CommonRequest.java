package com.alibaba.cloud.ai.example.graph.appagent;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommonRequest(

		/* 任务 */
		@JsonProperty(value = "input", defaultValue = "查一下还有哪些学生没续班") String input,
		/* 会话标识 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,
		/* 追加的任务信息。 */
		@JsonProperty(value = "feedback") String feedback) {

}
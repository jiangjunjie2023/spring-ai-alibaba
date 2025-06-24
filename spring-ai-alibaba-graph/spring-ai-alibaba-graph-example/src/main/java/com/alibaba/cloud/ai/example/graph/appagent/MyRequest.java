package com.alibaba.cloud.ai.example.graph.appagent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record MyRequest(

		/**
		 * 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程。
		 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,
		/**
		 * 最大计划迭代次数，用于控制处理请求的最大步骤数。 默认值为 1，表示至少执行一次完整流程。
		 */
		@JsonProperty(value = "max_plan_iterations", defaultValue = "1") Integer maxPlanIterations,
		/**
		 * 最大步骤数，用于控制单次请求的步骤数。 默认值为 3，表示最多执行 3 步。
		 */
		@JsonProperty(value = "max_step_num", defaultValue = "3") Integer maxStepNum,
		/**
		 * 是否自动接受计划，用于控制是否自动接受生成的计划。 默认值为 true，表示自动接受计划。
		 */
		@JsonProperty(value = "auto_accept_plan", defaultValue = "true") Boolean autoAcceptPlan,
		/**
		 * 中断反馈，用于控制中断后的反馈信息。
		 */
		@JsonProperty(value = "interrupt_feedback") String interruptFeedback,

		@JsonProperty(value = "query", defaultValue = "草莓蛋糕怎么做呀") String query) {
}
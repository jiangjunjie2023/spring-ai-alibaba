package com.alibaba.cloud.ai.example.graph.appagent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record MyRequest(

		/* 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程。 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,
		/* 最大计划迭代次数，用于控制处理请求的最大步骤数。 默认值为 1，表示至少执行一次完整流程。 */
		@JsonProperty(value = "max_plan_iterations", defaultValue = "1") Integer maxPlanIterations,
		/* 最大步骤数，用于控制单次请求的步骤数。 默认值为 3，表示最多执行 3 步。 */
		@JsonProperty(value = "max_step_num", defaultValue = "3") Integer maxStepNum,
		/* 中断反馈，用于控制中断后的反馈信息。 */
		@JsonProperty(value = "interrupt_tip") String interruptTip,
		/* 人类反馈信息。 */
		@JsonProperty(value = "feedback") String feedback,

		@JsonProperty(value = "query", defaultValue = "查一下还有哪些学生没续班") String query) {
}
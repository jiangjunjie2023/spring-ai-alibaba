/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/18 16:54
 */

public class HumanFeedbackNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws GraphInterruptException {
		logger.info("HumanFeedbackNode, 收到的完整state: {}", state.data());
		boolean interrupt = (Boolean) state.value("interrupt").orElse(false);
		logger.info("HumanFeedbackNode, interrupt: {}", interrupt);
		String interruptTip = (String) state.value("interrupt_tip").orElse("interrupt");
		logger.info("HumanFeedbackNode, interruptTip: {}", interruptTip);

		Map<String, Object> updated = new HashMap<>();
		// auto_accepted、yes、no 迭代次数都+1
		// updated.put("plan_iterations", getPlanIterations(state) + 1);

		// 需要反馈 且 未拿到反馈时 中断
		// 只有首次执行且没有人工反馈时才中断
		if (interrupt) {
			if (!state.isResume()) {// && state.humanFeedback() == null
				throw new GraphInterruptException(interruptTip);
			}
			else {
				updated.put("interrupt", false);
				updated.put("interrupt_tip", "");
			}
		}
		// 重置为非恢复状态
		state.withoutResume();
		Map<String, Object> feedBackData = state.humanFeedback().data();
		String feedback = feedBackData.getOrDefault("feedback", "").toString();
		logger.info("HumanFeedbackNode, feedback: {}", feedback);
		if (!StringUtils.isEmpty(feedback)) {
			updated.put("feedback", feedback);
		}
		logger.info("HumanFeedbackNode, updated: {}", updated);
		return updated;
	}

	private void interrupt(OverAllState state, String interruptTip) throws GraphInterruptException {
		// 只有首次执行且没有人工反馈时才中断
		if (!state.isResume() && state.humanFeedback() == null) {
			throw new GraphInterruptException(interruptTip);
		}
	}

	public static Integer getPlanIterations(OverAllState state) {
		return state.value("plan_iterations", 0);
	}

}

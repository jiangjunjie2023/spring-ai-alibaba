package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HumanFeedbackDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		String nextNode = state.value("human_next_node").orElse(false).toString();
		logger.info("HumanFeedbackDispatcher，nextNode={}", nextNode);
		return nextNode;
	}

}

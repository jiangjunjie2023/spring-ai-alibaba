package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntentionDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(IntentionDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		String input = (String) state.value("input").orElse("");
		logger.info("IntentionDispatcher, 收到input: {}", input);
		String branch = "invalid";
		if (input.contains("续班") || input.contains("报名")) {
			branch = "valid";
		}
		else {
			logger.info("IntentionDispatcher, 意图不符，跳出。input={}", input);
		}
		logger.info("IntentionDispatcher, next branch is {}", branch);
		return branch;
	}

}

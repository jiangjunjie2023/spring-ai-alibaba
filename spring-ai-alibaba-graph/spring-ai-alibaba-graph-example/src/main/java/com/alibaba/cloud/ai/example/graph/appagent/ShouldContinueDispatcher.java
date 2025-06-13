package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShouldContinueDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(ShouldContinueDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		Boolean isFinish = (Boolean) state.value("is_finish").orElse(false);
		logger.info("ShouldContinueDispatcher判断is_finish={}，分支={}", isFinish, isFinish ? "finish" : "continue");
		return isFinish ? "finish" : "continue";
	}

}

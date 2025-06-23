package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StartNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(StartNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("StartNode, 收到的完整state: {}", state.data());
		String input = (String) state.value("input").orElse("");
		logger.info("StartNode, 收到input: {}", input);

		String output = "start node finished.";

		Map<String, Object> updated = new HashMap<>();
		updated.put("output", output);
		logger.info("StartNode, 返回的updated: {}", updated);
		return updated;
	}

}

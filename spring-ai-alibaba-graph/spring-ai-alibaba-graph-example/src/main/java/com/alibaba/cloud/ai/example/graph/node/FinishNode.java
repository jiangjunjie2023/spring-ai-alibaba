package com.alibaba.cloud.ai.example.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FinishNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(FinishNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("state: {}", state.data());
		String output = (String) state.value("output").orElse("");
		logger.info("output: {}", output);

		ActionNode.AgentResponse response = new Gson().fromJson(output, ActionNode.AgentResponse.class);
		if (response == null || StringUtils.isEmpty(response.getAction())) {
			logger.info("未识别的结果，直接返回agentOutcome");
		}
		else if (response.getAction().equals("finalAnswer")) {
			output = response.getOutput();
		}
		else {
			output = "";
		}
		String finalOutput = StringUtils.isEmpty(output) ? "未查到数据" : output;
		logger.info("final_output: {}", output);

		Map<String, Object> updated = new HashMap<>();
		updated.put("final_output", output);
		logger.info("updated: {}", updated);
		return updated;
	}

}

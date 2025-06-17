package com.alibaba.cloud.ai.example.graph.appagent;

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
		logger.info("FinishNode收到的完整state: {}", state.data());
		String agentOutcome = (String) state.value("agent_outcome").orElse("");
		logger.info("FinishNode收到agent_outcome: {}", agentOutcome);

		String output = "";
		ActionNode.AgentResponse response = new Gson().fromJson(agentOutcome, ActionNode.AgentResponse.class);
		if (response == null || StringUtils.isEmpty(response.getAction())) {
			logger.info("未识别的结果，直接返回agentOutcome");
			output = agentOutcome;
		}
		else if (response.getAction().equals("finalAnswer")) {
			output = response.getOutput();
		}
		else {
			output = "";
		}
		logger.info("FinishNode返回: {}", output);

		Map<String, Object> updated = new HashMap<>();
		updated.put("final_output", output);
		logger.info("FinishNode返回的updated: {}", updated);
		return updated;
	}

}

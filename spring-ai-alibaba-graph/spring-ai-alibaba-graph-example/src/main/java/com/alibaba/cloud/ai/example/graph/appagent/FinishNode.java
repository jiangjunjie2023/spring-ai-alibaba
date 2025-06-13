package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

// 负责根据 agent_outcome 字段调用不同工具
public class FinishNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(FinishNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("FinishNode收到的完整state: {}", state.data());
		String agentOutcome = (String) state.value("agent_outcome").orElse("");
		logger.info("FinishNode收到agent_outcome: {}", agentOutcome);

		String output = "";
		if (agentOutcome.startsWith("Final Answer")) {
			// 解析参数
			String[] parts = agentOutcome.split(":");
			output = parts[1];
		}

		logger.info("FinishNode工具返回: {}", output);

		Map<String, Object> updated = new HashMap<>();
		updated.put("final_output", output);
		logger.info("FinishNode返回的updated: {}", updated);
		return updated;
	}

}

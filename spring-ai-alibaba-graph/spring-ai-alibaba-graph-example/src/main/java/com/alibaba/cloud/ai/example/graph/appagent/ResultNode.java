package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResultNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("ResultNode, 收到的完整state: {}", state.data());
		String input = "";
		Object outputObj = state.value("output").orElse(null);
		if (outputObj instanceof String) {
			// 若是字符串，直接转换
			input = (String) outputObj;
		}
		else if (outputObj instanceof ArrayList<?> list) {
			// 若是ArrayList，根据需求处理（例如拼接元素）
			// 将列表元素转为字符串（假设元素是字符串类型）
			input = list.stream().map(Object::toString).collect(Collectors.joining(", "));
		}
		else {
			// 其他类型处理
			input = outputObj != null ? outputObj.toString() : "";
		}

		logger.info("ResultNode, 收到input: {}", input);

		String output = "result:" + input;

		logger.info("ResultNode, 返回: {}", output);

		Map<String, Object> updated = new HashMap<>();
		updated.put("final_output", output);
		logger.info("ResultNode, 返回的updated: {}", updated);
		return updated;
	}

}

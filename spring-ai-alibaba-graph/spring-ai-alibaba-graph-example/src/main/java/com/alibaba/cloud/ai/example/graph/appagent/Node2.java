package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Node2 implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(Node2.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Node2, 收到的完整state: {}", state.data());
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

		logger.info("Node2, 收到input: {}", input);

		Thread.sleep(1000); // 模拟耗时1秒
		String output = "node2 output";

		Map<String, Object> updated = new HashMap<>();
		updated.put("output", output);
		logger.info("Node2, 返回的updated: {}", updated);
		return updated;
	}

}

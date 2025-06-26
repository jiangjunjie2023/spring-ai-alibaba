package com.alibaba.cloud.ai.example.graph.node.xuban;

import com.alibaba.cloud.ai.example.graph.node.ActionNode;
import com.alibaba.cloud.ai.example.graph.tool.TeacherClassesTool;
import com.alibaba.cloud.ai.example.graph.tool.TeacherInfoTool;
import com.alibaba.cloud.ai.example.graph.tool.XubanCheckTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XbNode2 implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(XbNode2.class);

	private final ChatClient chatClient;

	private final SystemPromptTemplate systemPromptTemplate;

	private static final String PROMPT = """
			按任务要求整理返回数据
			用户任务: {task}
			已知数据: {data}
			""";

	public XbNode2(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.systemPromptTemplate = new SystemPromptTemplate(PROMPT);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Node2, state: {}", state.data());
		String input = (String) state.value("input").orElse("");
		logger.info("Node2, input: {}", input);
		String output = (String) state.value("output").orElse("");
		logger.info("Node2, output: {}", output);
		// List observationList = state.value("observation", List.class).orElse(new
		// ArrayList<>());
		String observation = state.value("observation").orElse("").toString();
		logger.info("Node2, observation: {}", observation);

		ActionNode.AgentResponse response = new Gson().fromJson(output, ActionNode.AgentResponse.class);
		if (response == null || StringUtils.isEmpty(response.getAction())
				|| !response.getAction().equals("xuban_check")) {
			logger.warn("未识别的结果，直接返回");
			Map<String, Object> updated = new HashMap<>();
			updated.put("output", "未查询到数据");
			logger.info("Node2, updated: {}", updated);
			return updated;
		}

		Map<String, Object> params = response.getParams();
		Double schoolId = (Double) params.get("schoolId");
		String teacherCode = (String) params.get("teacherCode");
		String classCode = (String) params.get("classCode");
		logger.info("调用XubanCheckTool, 参数: teacherCode={}, schoolId={}, classCode={}", teacherCode, schoolId,
				classCode);
		String result = XubanCheckTool.xubanCheck(teacherCode, schoolId.intValue(), classCode);

		String prompt = systemPromptTemplate.render(Map.of("task", input, "data", result));
		logger.info("Node2, prompt: {}", prompt);

		result = chatClient.prompt().user(prompt).call().content();
		logger.info("Node2, result: {}", result);

		Map<String, Object> updated = new HashMap<>();
		updated.put("output", result);
		logger.info("Node2, 返回的updated: {}", updated);
		return updated;
	}

}

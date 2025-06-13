package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 负责调用大模型，生成 action 或 finish
public class AgentNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(AgentNode.class);

	private final ChatClient chatClient;

	private SystemPromptTemplate systemPromptTemplate;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final String AGENT_PROMPT_2 = """
			用户以工作流的方式处理任务，处理过程中有以下工具可以使用：
			{tools}

			要求尽可能以有效且准确的方式回应用户。以思维链的方式来决策工具的使用，遵循以下格式：
			问题: 待回答的输入问题
			思考: 考虑需要执行的操作
			操作: 下一步行动
			观察: 观察操作的结果

			... (可以重复n次 思考/曹组/观察)

			使用 JSON 块来指定工具，其中action代表下一步行动，有效的action值为："finalAnswer" 或者是 指定的工具名。
			1. 当思考得到最终回复结果时，输出action为finalAnswer，示例如下：
			{finishJson}

			2. 当思考未得到最终回复结果时，输出要执行的工具，示例如下：
			{actionJson}

			开始！请记住每个步骤都要包含思考、操作和观察三个部分，并以JSON描述好下一步行动进行响应。
			用户问题: {input}
			""";
	// 完成示例json
	private static final String FINISH_JSON_EXAMPLE = """
		{
			"thought": "任务已完成",
			"action": "finalAnswer",
			"output": "回复结果"
		}
		""";
	// 操作示例json
	private static final String ACTION_JSON_EXAMPLE = """
		{
			"thought": "考虑需要执行的操作",
			"action": "xuban_check",
			"params": {
				"teacherCode": "",
				"schoolId": "",
				"classCode": ""
			}
		}
		""";

	// 1. 定义 Prompt 模板
	private static final String AGENT_PROMPT = "You are a helpful AI assistant. You have access to the following tools:\n\n"
			+ "%s\n\n" + // toolsDesc
			"When you receive a question, you should:\n" + "- Think step by step about how to answer.\n"
			+ "- If you need to use a tool, call it with the right parameters.\n"
			+ "- If you use a tool, observe the result and continue reasoning.\n"
			+ "- If you do not need to use a tool, answer directly.\n" + "- 切记，不要编造信息.\n\n"
			+ "Use the following format:\n\n" + "Question: the input question you must answer\n"
			+ "Thought: you should always think about what to do\n"
			+ "Action: the action to take, should be one of [%s]\n" + // toolNames
			"Action Input: the input to the action\n" + "Observation: the result of the action\n"
			+ "... (this Thought/Action/Action Input/Observation can repeat N times)\n"
			+ "Thought: I now know the final answer\n" + "Final Answer: the final answer to the original question\n\n"
			+ "Begin!\n\n" + "Question: %s\n" + // userInput
			"Thought:";

	public AgentNode(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.systemPromptTemplate = new SystemPromptTemplate(AGENT_PROMPT_2);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String input = (String) state.value("input").orElse("");
		List<Map<String, String>> chatHistory = (List<Map<String, String>>) state.value("chat_history")
			.orElse(new ArrayList<>());

		logger.info("AgentNode收到input: {}", input);
		logger.info("AgentNode历史消息: {}", chatHistory);
		logger.info("AgentNode收到的完整state: {}", state.data());

		// 2. 组装工具描述和工具名
		String toolsDesc = "student_info: 查询学生信息，参数：schoolId, studentCode, features\n"
				+ "xuban_check: 查询续班情况，参数：teacherCode, schoolId, classCode\n"
				+ "teacher_info: 查询教师身份，参数：email, e2e, e2mf";

		// 3. 拼接最终 prompt
		String prompt = systemPromptTemplate.render(Map.of("tools", toolsDesc, "input", input));
		logger.info("AgentNode, prompt: {}", prompt);
		// 4. 可选：拼接历史消息（如有）
		if (chatHistory != null && !chatHistory.isEmpty()) {
			StringBuilder history = new StringBuilder();
			for (Map<String, String> msg : chatHistory) {
				history.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
			}
			prompt = history + prompt;
		}

		logger.info("AgentNode最终Prompt: {}", prompt);

		// 调用大模型
		String result = chatClient.prompt().user(prompt).call().content();

		logger.info("AgentNode LLM返回: {}", result);

		// 解析Action和Action Input
		String toolCall = parseToolCall(result);

		// 简单判断是否结束（可根据实际业务调整）
		boolean isFinish = result.contains("finalAnswer");

		Map<String, Object> updated = new HashMap<>();
		updated.put("agent_outcome", toolCall != null ? toolCall : result);
		updated.put("is_finish", isFinish);

		// 更新 chat_history
		List<Map<String, String>> newHistory = new ArrayList<>(chatHistory);
		Map<String, String> userMsg = new HashMap<>();
		userMsg.put("role", "user");
		userMsg.put("content", input);
		newHistory.add(userMsg);

		Map<String, String> assistantMsg = new HashMap<>();
		assistantMsg.put("role", "assistant");
		assistantMsg.put("content", result);
		newHistory.add(assistantMsg);

		updated.put("chat_history", newHistory);

		logger.info("AgentNode返回的updated: {}", updated);

		return updated;
	}

	/**
	 * 解析 LLM 返回内容，提取 Action 和 Action Input 并拼成 TOOL:xxx|key1=val1|key2=val2 格式
	 */
	private String parseToolCall(String llmOutput) {
		try {
			Pattern actionPattern = Pattern.compile("Action:\\s*(TOOL:[\\w_]+)");
			Pattern inputPattern = Pattern.compile("Action Input:\\s*(\\{.*?\\})", Pattern.DOTALL);

			Matcher actionMatcher = actionPattern.matcher(llmOutput);
			Matcher inputMatcher = inputPattern.matcher(llmOutput);

			if (actionMatcher.find() && inputMatcher.find()) {
				String tool = actionMatcher.group(1); // e.g. TOOL:teacher_info
				String inputJson = inputMatcher.group(1); // e.g. {"email": "...", ...}
				Map<String, Object> paramMap = objectMapper.readValue(inputJson, Map.class);

				StringBuilder sb = new StringBuilder(tool);
				for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
					sb.append("|").append(entry.getKey()).append("=").append(entry.getValue());
				}
				logger.info("解析到工具调用: {}", sb);
				return sb.toString();
			}
		}
		catch (Exception e) {
			logger.error("解析LLM工具调用失败: {}", e.getMessage(), e);
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		String toolsDesc = "student_info: 查询学生信息，参数：schoolId, studentCode, features\n"
				+ "xuban_check: 查询续班情况，参数：teacherCode, schoolId, classCode\n"
				+ "teacher_info: 查询教师身份，参数：email, e2e, e2mf";
		String input = "输入";
		// 渲染时添加变量
		Map<String, Object> model = new HashMap<>();
		model.put("tools", toolsDesc);
		model.put("input", input);
		model.put("finishJson", FINISH_JSON_EXAMPLE);
		model.put("actionJson", ACTION_JSON_EXAMPLE);

		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(AGENT_PROMPT_2);
		String prompt = systemPromptTemplate.render(model);
		System.out.println("AgentNode, prompt: " + prompt);
	}
}

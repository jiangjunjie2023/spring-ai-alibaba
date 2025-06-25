package com.alibaba.cloud.ai.example.graph.node.xuban;

import com.alibaba.cloud.ai.example.graph.node.ActionNode;
import com.alibaba.cloud.ai.example.graph.tool.TeacherClassesTool;
import com.alibaba.cloud.ai.example.graph.tool.TeacherInfoTool;
import com.alibaba.cloud.ai.example.graph.tool.XubanCheckTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XbNode1 implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(XbNode1.class);

	private final ChatClient chatClient;

	private final SystemPromptTemplate systemPromptTemplate;

	private static final String PROMPT = """
			用户任务: {input}
			解决该任务可以使用的工具：
			{tools}

			用户需经过多步工具调用来完成任务。要求你辅助用户选出当前步骤需要调用的工具，用户会根据操作指引完成工具的调用，并将操作结果再放入已知数据。
			已知数据：{currentContext}

			遵循以下过程决策：
			观察: 观察已知数据，来进入下一轮思考
			思考: 考虑需要执行的操作
			操作: 使用JSON明确告知用户下一步行动，有效的action值为："finalAnswer" 或者是 指定的工具名。
			1. 当思考结果是要输出结果时，输出action为finalAnswer，示例如下：
			{finishJson}
			2. 当思考结果是要调用工具时，输出要执行的工具，示例如下：
			{actionJson}
			JSON块仅包含内容，不要```json来包裹。JSON中的value为数字的用Integer格式返回。

			开始！请记住每一轮都要包含思考、操作和观察三个部分，输出下一步行动描述时仅包含JSON内容，便于解析。严禁杜撰数据。
			""";

	private static final String PROMPT_2 = """
			判断工具返回的结果数据中有几个班号
			工具返回的数据如下：
			{toolResult}

			记住，仅返回班号数量，不确定或者没有则返回0
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

	public XbNode1(ChatClient chatClient) {
		this.chatClient = chatClient;
		this.systemPromptTemplate = new SystemPromptTemplate(PROMPT);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws GraphInterruptException {
		logger.info("Node1, state: {}", state.data());
		logger.info("Node1, resume: {}", state.isResume());
		logger.info("Node1, human: {}", state.humanFeedback());
		String input = (String) state.value("input").orElse("");
		logger.info("Node1, input: {}", input);
		String observation = (String) state.value("observation").orElse("");
		logger.info("Node1, observation: {}", observation);
		String feedback = (String) state.value("feedback").orElse("");
		logger.info("Node1, feedback: {}", feedback);

		Map<String, Object> updated = new HashMap<>();
		if (StringUtils.isEmpty(feedback)) {
			Map<String, Object> map = classCodeList(input);
			observation += (String) map.get("observation");
			String action = (String) map.get("action");
			String finalOutput = (String) map.get("final_output");
			if (action.equals("finalAnswer")) {
				updated.put("final_output", finalOutput);
				logger.info("Node1, 返回的updated: {}", updated);
				return updated;
			}
			List<String> classCodeList = (List<String>) map.get("toolResult");
			logger.info("Node1, classCodeList: {}", classCodeList);
			// 带班量大于1且未指定班级 需要指定班级
			if (CollectionUtils.isEmpty(classCodeList)) {
				logger.info("Node1, 没有可查信息");
				updated.put("output", "没有可查信息");
				return updated;
			}
			else if (classCodeList.size() > 1) {
				String interrupt_tip = "想要查询哪个班呢?" + StringUtils.join(classCodeList, ",");
				updated.put("interrupt_tip", interrupt_tip);
				logger.info("Node1, interrupt_tip={}", interrupt_tip);
				interrupt(state, interrupt_tip);
			}
			else {
				observation += ", 指定的班号是" + classCodeList.get(0);
			}
		} else {
			observation += ", 指定的班号是" + feedback;
		}

		observation = forNext(input, observation);
		updated.put("observation", observation);// tool params json for next node
		logger.info("Node1, 返回的updated: {}", updated);
		return updated;
	}

	private Map<String, Object> classCodeList(String input) {
		Map<String, Object> updated = new HashMap<>();
		// 查询身份信息
		String email = "zhouchanghua@xdf.cn";
		String e2e = "21D9AAB155F13810995FD41F3088F4A1";
		String e2mf = "66c180202a4f4a45869b0231cb59522b";
		logger.info("Node1, 调用TeacherInfoTool, 参数: email={}, e2e={}, e2mf={}", email, e2e, e2mf);
		String identity = TeacherInfoTool.queryTeacherIdentity(email, e2e, e2mf);

		// 组装工具描述和工具名
		String toolsDesc = "student_info: 查询学生信息，参数：schoolId, studentCode, features\n"
				+ "teacher_classes: 查询续班情况，参数：teacherCode, schoolId";

		// 渲染时添加变量
		Map<String, Object> model = new HashMap<>();
		model.put("tools", toolsDesc);
		model.put("input", input);
		model.put("finishJson", FINISH_JSON_EXAMPLE);
		model.put("actionJson", ACTION_JSON_EXAMPLE);

		model.put("currentContext", identity);
		// 查
		String prompt = systemPromptTemplate.render(model);
		logger.info("Node1, prompt: {}", prompt);

		// 调用大模型
		String result = chatClient.prompt().user(prompt).call().content();

		logger.info("Node1 LLM返回: {}", result);

		ActionNode.AgentResponse response = new Gson().fromJson(result, ActionNode.AgentResponse.class);
		if (response == null || StringUtils.isEmpty(response.getAction())) {
			logger.warn("未识别的结果，直接返回");
			return null;
		}
		else {
			updated.put("action", response.getAction());
			if (response.getAction().equals("finalAnswer")) {
				updated.put("final_output", response.getOutput());
				return updated;
			}
			else if (!response.getAction().equals("teacher_classes")) {
				updated.put("final_output", response.getOutput());
				return updated;
			}
		}

		Map<String, Object> params = response.getParams();
		Double schoolId = null;
		Object originSchoolId = params.get("schoolId");
		if (originSchoolId != null) {
			try {
				schoolId = originSchoolId instanceof Number ? ((Number) originSchoolId).doubleValue()
						: Double.parseDouble(originSchoolId.toString());
			}
			catch (NumberFormatException e) {
				logger.error("schoolId必须为数字类型");
			}
		}
		String teacherCode = (String) params.get("teacherCode");
		Integer finalSchoolId = schoolId == null ? 0 : schoolId.intValue();
		List<String> toolResult = TeacherClassesTool.queryClasses(teacherCode, finalSchoolId);
		logger.info("toolResult: {}", toolResult);
		updated.put("toolResult", toolResult);
		updated.put("observation", identity);
		return updated;
	}

	private String forNext(String input, String observation) {
		// 查询身份信息
		logger.info("forNext, input={}, observation={}", input, observation);
		// 组装工具描述和工具名
		String toolsDesc = "xuban_check: 查询学生信息，参数：schoolId, teacherCode, classCode";

		// 渲染时添加变量
		Map<String, Object> model = new HashMap<>();
		model.put("tools", toolsDesc);
		model.put("input", input);
		model.put("finishJson", FINISH_JSON_EXAMPLE);
		model.put("actionJson", ACTION_JSON_EXAMPLE);

		model.put("currentContext", observation);
		String prompt = systemPromptTemplate.render(model);
		logger.info("forNext, prompt: {}", prompt);

		// 调用大模型
		String result = chatClient.prompt().user(prompt).call().content();
		logger.info("forNext LLM返回: {}", result);
		return result;
	}

	private void interrupt(OverAllState state, String tip) throws GraphInterruptException {
		// 只有首次执行且没有人工反馈时才中断
		if (!state.isResume() && state.humanFeedback() == null) {
			throw new GraphInterruptException(tip);
		}
	}

}

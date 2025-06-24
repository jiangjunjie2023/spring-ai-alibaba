package com.alibaba.cloud.ai.example.graph.node;

import com.alibaba.cloud.ai.example.graph.tool.StudentInfoTool;
import com.alibaba.cloud.ai.example.graph.tool.TeacherInfoTool;
import com.alibaba.cloud.ai.example.graph.tool.XubanCheckTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

// 负责根据 agent_outcome 字段调用不同工具
public class ActionNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ActionNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("ActionNode收到的完整state: {}", state.data());
		String agentOutcome = (String) state.value("agent_outcome").orElse("");
		logger.info("ActionNode收到agent_outcome: {}", agentOutcome);

		String observation;

		AgentResponse response = new Gson().fromJson(agentOutcome, AgentResponse.class);
		if (response == null || StringUtils.isEmpty(response.getAction())) {
			logger.info("未识别的结果，直接返回agentOutcome");
			observation = "[TOOL_RESULT] " + agentOutcome;

			logger.info("ActionNode工具返回: {}", observation);

			Map<String, Object> updated = new HashMap<>();
			updated.put("observation", observation);
			logger.info("ActionNode返回的updated: {}", updated);
			return updated;
		}

		if (response.getAction().equals("student_info")) {
			// 解析参数
			Map<String, Object> params = response.getParams();
			String schoolId = (String) params.get("schoolId");
			String studentCode = (String) params.get("studentCode");
			String features = (String) params.get("features");
			logger.info("调用StudentInfoTool, 参数: schoolId={}, studentCode={}, features={}", schoolId, studentCode,
					features);
			observation = StudentInfoTool.getStudentInfo(schoolId, studentCode, features);
		}
		else if (response.getAction().equals("xuban_check")) {
			Map<String, Object> params = response.getParams();
			// agent返回了Double类型
			Double schoolId = (Double) params.get("schoolId");
			String teacherCode = (String) params.get("teacherCode");
			String classCode = (String) params.get("classCode");
			logger.info("调用XubanCheckTool, 参数: teacherCode={}, schoolId={}, classCode={}", teacherCode, schoolId,
					classCode);
			observation = XubanCheckTool.xubanCheck(teacherCode, schoolId.intValue(), classCode);
		}
		else if (response.getAction().equals("teacher_info")) {
			Map<String, Object> params = response.getParams();
			String email = (String) params.get("email");
			String e2e = (String) params.get("e2e");
			String e2mf = (String) params.get("e2mf");
			logger.info("调用TeacherInfoTool, 参数: email={}, e2e={}, e2mf={}", email, e2e, e2mf);
			observation = TeacherInfoTool.queryTeacherIdentity(email, e2e, e2mf);
		}
		else if (response.getAction().equals("finalAnswer")) {
			logger.info("已完成全部调用，结果={}", response.getOutput());
			observation = response.getOutput();
		}
		else {
			logger.info("未识别的工具调用，直接返回agentOutcome");
			observation = "[TOOL_RESULT] " + agentOutcome;
		}

		logger.info("ActionNode工具返回: {}", observation);

		Map<String, Object> updated = new HashMap<>();
		updated.put("observation", observation);
		logger.info("ActionNode返回的updated: {}", updated);
		return updated;
	}

	@Data
	public static class AgentResponse {

		private String thought;

		private String action;

		private String output;

		private Map<String, Object> params;

	}

}

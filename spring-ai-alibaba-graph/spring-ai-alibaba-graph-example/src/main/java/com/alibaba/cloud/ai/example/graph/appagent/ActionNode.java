package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

// 负责根据 agent_outcome 字段调用不同工具
public class ActionNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ActionNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("ActionNode收到的完整state: {}", state.data());
		String agentOutcome = (String) state.value("agent_outcome").orElse("");
		logger.info("ActionNode收到agent_outcome: {}", agentOutcome);

		String observation;

		// 这里假设 agentOutcome 以"TOOL:xxx|参数1=...|参数2=..."格式传递
		if (agentOutcome.startsWith("TOOL:student_info")) {
			// 解析参数
			String[] parts = agentOutcome.split("\\|");
			String schoolId = getValue(parts, "schoolId");
			String studentCode = getValue(parts, "studentCode");
			String features = getValue(parts, "features");
			logger.info("调用StudentInfoTool, 参数: schoolId={}, studentCode={}, features={}", schoolId, studentCode,
					features);
			observation = StudentInfoTool.getStudentInfo(schoolId, studentCode, features);
		}
		else if (agentOutcome.startsWith("TOOL:xuban_check")) {
			String[] parts = agentOutcome.split("\\|");
			String teacherCode = getValue(parts, "teacherCode");
			String schoolId = getValue(parts, "schoolId");
			String classCode = getValue(parts, "classCode");
			logger.info("调用XubanCheckTool, 参数: teacherCode={}, schoolId={}, classCode={}", teacherCode, schoolId,
					classCode);
			observation = XubanCheckTool.xubanCheck(teacherCode, schoolId, classCode);
		}
		else if (agentOutcome.startsWith("TOOL:teacher_info")) {
			String[] parts = agentOutcome.split("\\|");
			String email = getValue(parts, "email");
			String e2e = getValue(parts, "e2e");
			String e2mf = getValue(parts, "e2mf");
			logger.info("调用TeacherInfoTool, 参数: email={}, e2e={}, e2mf={}", email, e2e, e2mf);
			observation = TeacherInfoTool.queryTeacherIdentity(email, e2e, e2mf);
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

	private String getValue(String[] parts, String key) {
		for (String part : parts) {
			if (part.trim().startsWith(key + "=")) {
				return part.trim().substring((key + "=").length());
			}
		}
		return "";
	}

}

package com.alibaba.cloud.ai.example.graph.appagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class StudentInfoTool {

	private static final Logger logger = LoggerFactory.getLogger(StudentInfoTool.class);

	private static final String BASE_URL = "https://k12-api.xdf.cn";

	public static String getStudentInfo(String schoolId, String studentCode, String features) {
		String apiUrl = BASE_URL + "/api/students/info";
		RestTemplate restTemplate = new RestTemplate();

		try {
			logger.info("请求学生信息API: schoolId={}, studentCode={}, features={}", schoolId, studentCode, features);
			ResponseEntity<Map> response = restTemplate.getForEntity(
					apiUrl + "?schoolId={schoolId}&studentCode={studentCode}&features={features}", Map.class, schoolId,
					studentCode, features == null ? "" : features);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Object> body = response.getBody();
				if (body.containsKey("data")) {
					logger.info("学生信息API返回: {}", body.get("data"));
					return "学生信息: " + body.get("data");
				}
				else {
					logger.warn("学生信息API无data字段: {}", body);
					return "No student data found in the response.";
				}
			}
			else {
				logger.error("学生信息API请求失败: {}", response.getStatusCode());
				return "Error occurred: " + response.getStatusCode();
			}
		}
		catch (Exception e) {
			logger.error("学生信息API异常: {}", e.getMessage(), e);
			return "Exception: " + e.getMessage();
		}
	}

}

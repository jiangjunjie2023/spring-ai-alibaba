package com.alibaba.cloud.ai.example.graph.appagent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class XubanCheckTool {

	private static final String BASE_URL = "https://k12-api.xdf.cn";

	public static String xubanCheck(String teacherCode, String schoolId, String classCode) {
		String url = BASE_URL + "/api/teachers/xuban/v1/classDetail?key=xuban" + "&teacherCode=" + teacherCode
				+ "&schoolId=" + schoolId + "&classCode=" + classCode + "&roleType=1";
		RestTemplate restTemplate = new RestTemplate();

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Object> json = response.getBody();
				if (Objects.equals(json.get("status"), 100000)) {
					Map<String, Object> statisticsData = (Map<String, Object>) ((Map<String, Object>) json.get("data"))
						.get("statisticsData");
					List<Object> activityList = (List<Object>) statisticsData.get("activityList");
					List<Map<String, Object>> stuGroupList = new ArrayList<>();
					for (Map<String, Object> group : (List<Map<String, Object>>) statisticsData.get("stuGroupList")) {
						if ("10".equals(group.get("groupType"))) {
							Map<String, Object> newGroup = new HashMap<>();
							newGroup.put("groupType", group.get("groupType"));
							newGroup.put("groupName", group.get("groupName"));
							newGroup.put("stuCnt", group.get("stuCnt"));
							List<Object> newStuList = new ArrayList<>();
							for (Map<String, Object> student : (List<Map<String, Object>>) group.get("stuList")) {
								Map<String, Object> newStudent = new HashMap<>();
								Map<String, Object> studentInfo = (Map<String, Object>) student.get("studentInfo");
								newStudent.put("studentInfo", Map.of("studentName", studentInfo.get("studentName"),
										"studentCode", studentInfo.get("studentCode")));
								newStudent.put("xubanShowInfos", student.get("xubanShowInfos"));
								newStuList.add(newStudent);
							}
							newGroup.put("stuList", newStuList);
							stuGroupList.add(newGroup);
						}
					}
					Map<String, Object> filtered = new HashMap<>();
					filtered.put("activityList", activityList);
					filtered.put("stuGroupList", stuGroupList);
					return "续班情况: " + filtered;
				}
				else {
					return "API返回异常: " + json;
				}
			}
			else {
				return "Error occurred: " + response.getStatusCode();
			}
		}
		catch (Exception e) {
			return "Exception: " + e.getMessage();
		}
	}

}

package com.alibaba.cloud.ai.example.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class XubanCheckTool {

	private static final String BASE_URL = "https://k12-api.xdf.cn";

	public static String xubanCheck(String teacherCode, Integer schoolId, String classCode) {
		String url = BASE_URL + "/api/teachers/xuban/v1/classDetail?teacherCode=" + teacherCode + "&schoolId="
				+ schoolId + "&classCode=" + classCode + "&roleType=1";
		log.info("xubanCheck, url={}", url);
		RestTemplate restTemplate = new RestTemplate();

		String result = "";
		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
			log.info("xubanCheck, response={}", response);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Object> json = response.getBody();
				if (Objects.equals(json.get("status"), 100000)) {
					Map<String, Object> statisticsData = (Map<String, Object>) ((Map<String, Object>) json.get("data"))
						.get("statisticsData");
					log.info("xubanCheck, statisticsData={}", statisticsData);
					List<Object> activityList = (List<Object>) statisticsData.get("activityList");
					log.info("xubanCheck, activityList={}", activityList);
					List<Map<String, Object>> originList = (List<Map<String, Object>>) statisticsData
						.get("stuGroupList");
					List<Map<String, Object>> stuGroupList = new ArrayList<>();
					for (Map<String, Object> group : originList) {
						// 1 已加购, 2 已报名, 10 为续班
						if ("2".equals(group.get("groupType"))) {
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
					return "报名结果: " + filtered;
				}
				else {
					log.error("xubanCheck, error: {}", "API返回异常");
				}
			}
			else {
				log.error("xubanCheck, error: {}", response.getBody());
			}
		}
		catch (Exception e) {
			log.error("xubanCheck, exception: {}", e.getMessage());
		}
		return result;
	}

}

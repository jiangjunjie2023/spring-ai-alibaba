package com.alibaba.cloud.ai.example.graph.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class TeacherClassesTool {

	private static final String BASE_URL = "https://one-piece.staff.xdf.cn";

	public static List<String> queryClasses(String teacherCode, Integer schoolId) {
		String url = BASE_URL + "/teacher/classes/extand?teacherCode=" + teacherCode + "&classState=0&schoolId="
				+ schoolId + "&features=needSmallGroupLesson,needMotherSonClass,needGroupClass,needNormalClass"
				+ "&pageNo=1&pageSize=100&sortType=1&finishClassStartTime=2025-04-17";
		log.info("queryClasses, url={}", url);
		RestTemplate restTemplate = new RestTemplate();

		List<String> classCodes = new ArrayList<>();
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			log.info("queryClasses, response={}", response);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				// 1. 解析JSON
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(response.getBody());
				boolean status = root.path("isSuccess").asBoolean();
				System.out.println(status);
				if (status) {
					JsonNode rows = root.path("data").path("rows");
					// 2. 遍历提取所有classCode
					for (JsonNode row : rows) {
						classCodes.add(row.get("classCode").asText());
					}
					// 3. 输出结果
					log.info("All ClassCodes: " + classCodes); // 输出: [P25QY1110Q4,
																// ABC123]
				}
				else {
					log.error("queryClasses, error: {}", "API返回异常");
				}
			}
			else {
				log.error("queryClasses, error: {}", response.getBody());
			}
		}
		catch (Exception e) {
			log.error("queryClasses, exception: {}", e.getMessage());
		}
		return classCodes;
	}

	public static void main(String[] args) throws JsonProcessingException {
		String json = "{\"status\":\"1\",\"message\":\"success\",\"data\":{\"no\":1,\"size\":100,\"offset\":0,\"condition\":null,\"totalRowCount\":7,\"pageCount\":1,\"rows\":[{\"schoolId\":10,\"className\":\"8EJ暑假成长学习托管\",\"classCode\":\"8WLJS2606\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-07-14 10:30:00\",\"classEndTime\":\"2025-07-25 12:30:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":25,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"杨家坪盾安九龙都外租校区\",\"classSprintTime\":\"10:30-12:30\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"25\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EZ暑假成长学习托管\",\"classCode\":\"8WLZS2606\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-07-14 16:10:00\",\"classEndTime\":\"2025-07-25 18:10:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":25,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"杨家坪盾安九龙都外租校区\",\"classSprintTime\":\"16:10-18:10\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"25\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EJ暑假成长学习托管\",\"classCode\":\"8WLJS2612\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-07-28 08:10:00\",\"classEndTime\":\"2025-08-08 10:10:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":25,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"杨家坪盾安九龙都外租校区\",\"classSprintTime\":\"08:10-10:10\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"25\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EZ暑假成长学习托管\",\"classCode\":\"8WLZS2607\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-07-28 10:30:00\",\"classEndTime\":\"2025-08-08 12:30:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":25,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"杨家坪盾安九龙都外租校区\",\"classSprintTime\":\"10:30-12:30\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"25\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EZ暑假成长学习托管\",\"classCode\":\"8WLZS2610\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-07-28 16:10:00\",\"classEndTime\":\"2025-08-08 18:10:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":23,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"南坪嘉发外租校区\",\"classSprintTime\":\"16:10-18:10\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"23\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EJ暑假成长学习托管定金\",\"classCode\":\"8WLJS2615\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-08-11 13:50:00\",\"classEndTime\":\"2025-08-22 15:50:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":5,\"lessonCount\":12,\"realLessonCount\":0,\"lessonNo\":1,\"printAddress\":\"三峡广场重师生地楼\",\"classSprintTime\":\"13:50-15:50\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"J\",\"classSystemName\":\"计费体系\",\"studentNormalCount\":\"5\",\"classTagCode\":18,\"classTag\":\"JF1\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"J\",\"isNet\":1,\"sonClass\":false},{\"schoolId\":10,\"className\":\"8EJ暑假成长学习托管\",\"classCode\":\"8WLHS2607\",\"deptCode\":\"36\",\"deptName\":\"智慧学习部\",\"gradeCode\":\"3699\",\"gradeName\":\"通用\",\"subjectCode\":\"4\",\"subjectName\":\"物理\",\"classStartTime\":\"2025-08-11 16:10:00\",\"classEndTime\":\"2025-08-22 18:10:00\",\"quarterCode\":\"2\",\"quarterName\":\"暑假\",\"studentCount\":24,\"lessonCount\":12,\"realLessonCount\":12,\"lessonNo\":1,\"printAddress\":\"三峡广场重师生地楼\",\"classSprintTime\":\"16:10-18:10\",\"season\":\"暑假\",\"bizType\":\"DEFAULT\",\"groupClassNos\":[],\"dataSourceType\":\"JW\",\"teachingMethod\":\"3\",\"classState\":3,\"originDeptCode\":\"36\",\"stuMaxCount\":25,\"classSystemCode\":\"P\",\"classSystemName\":\"常规体系\",\"studentNormalCount\":\"24\",\"classTagCode\":419,\"classTag\":\"TG\",\"stdDeptCode\":\"36\",\"managementProjectCode\":\"mp3303\",\"managementProjectName\":\"学习机托管\",\"serviceType\":\"K\",\"isNet\":1,\"sonClass\":false}],\"rowCount\":7,\"lastPage\":true,\"firstPage\":true},\"show\":false,\"isSuccess\":true,\"traceId\":\"maFQrbkYjK0GydOQ\",\"errorCode\":\"100000\",\"tid\":\"0df6d44d98ce464fb01beba9bdd8b8fa.168.17508450624809597\"}";
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(json);
		boolean status = root.path("isSuccess").asBoolean();
		System.out.println(status);
		if (status) {
			JsonNode rows = root.path("data").path("rows");
			for (JsonNode row : rows) {
				System.out.println(row.get("classCode").asText());
			}
		}
	}

}

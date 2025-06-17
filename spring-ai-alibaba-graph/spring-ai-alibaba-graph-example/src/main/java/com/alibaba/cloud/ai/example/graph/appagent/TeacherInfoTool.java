package com.alibaba.cloud.ai.example.graph.appagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
public class TeacherInfoTool {

	private static final String BASE_URL = "https://k12-api.xdf.cn";

	public static String queryTeacherIdentity(String email, String e2e, String e2mf) {
		String url = BASE_URL + "/api/teachers/teacherInfo/queryIdentity?email=" + email;
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("e2e", e2e);
		headers.set("e2mf", e2mf);

		String result = "";
		HttpEntity<String> entity = new HttpEntity<>(headers);
		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return "教师身份: " + response.getBody();
			}
			else {
				log.error("queryTeacherIdentity, error: {}", response.getBody());
			}
		}
		catch (Exception e) {
			log.error("queryTeacherIdentity, exception: {}", e.getMessage());
		}
		return result;
	}

}

package com.alibaba.cloud.ai.example.graph.appagent;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class TeacherInfoTool {

	private static final String BASE_URL = "https://k12-api.xdf.cn";

	public static String queryTeacherIdentity(String email, String e2e, String e2mf) {
		String url = BASE_URL + "/api/teachers/teacherInfo/queryIdentity?email=" + "yueqianqian@xdf.cn";
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("e2e", "894DC59F52530720C11F2140AFC8F701");
		headers.set("e2mf", "f9019ccb148e4e98acf71fbcc09b4958");
		// headers.set("e2e", e2e);
		// headers.set("e2mf", e2mf);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return "教师身份: " + response.getBody();
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

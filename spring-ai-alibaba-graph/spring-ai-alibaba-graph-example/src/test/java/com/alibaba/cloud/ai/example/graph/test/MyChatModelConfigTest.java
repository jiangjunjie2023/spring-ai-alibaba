package com.alibaba.cloud.ai.example.graph.test;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Qualifier;

@SpringBootTest
public class MyChatModelConfigTest {

	@Autowired
	@Qualifier("myChatModel")
	private ChatModel myChatModel;

	@Test
	public void testChatModel() {
		String prompt = "你是谁？";
		String result = myChatModel.call(prompt);
		System.out.println("模型返回: " + result);
		assert result != null && !result.isEmpty();
	}

	@Test
	public void testChatClient() {
		String prompt = "你是谁？";
		ChatClient chatClient = ChatClient.builder(myChatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
		String result = chatClient.prompt().user(prompt).call().content();
		System.out.println("模型返回: " + result);
		assert result != null && !result.isEmpty();
	}

}
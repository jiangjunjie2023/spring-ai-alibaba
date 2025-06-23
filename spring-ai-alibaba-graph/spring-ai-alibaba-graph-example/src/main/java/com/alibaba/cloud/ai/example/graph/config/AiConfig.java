/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.graph.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

	/**
	 * Currently, two ChatModels are introduced, OpenAiChatModel and DashScopeChatModel.
	 * By default, OpenAiChatModel is specified, otherwise the program will report an
	 * error.
	 * @param openAiChatModel
	 * @return ChatModel
	 */
	@Bean
	@Primary
	public ChatModel preferredChatModel(OpenAiChatModel openAiChatModel) {
		return openAiChatModel;
	}

	@Bean("myChatModel")
	public ChatModel myChatModel() {
		OpenAiApi openAiApi = OpenAiApi.builder()
			// https://xone-txgw1.test.xdf.cn/ai-pawn-t2-5701
			.baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
			// test 19c275910fc749dcb4479b6c4608566a
			// pro 73847ef6649c9ec85ac147ecf1330cd0
			// ali sk-ec63ec37f5ec442f9e446eb7fc87ff49
			.apiKey(new SimpleApiKey("sk-ec63ec37f5ec442f9e446eb7fc87ff49"))
			.completionsPath("/v1/chat/completions")
			.embeddingsPath("/v1/embeddings")
			.build();
		// 豆包 ep-20240918193656-wn85w
		// 门神 gpt-4o
		// ali qwen-max-latest
		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
			.model("qwen-max-latest")
			.temperature(0.8)
			.build();
		return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
	}

	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.build();
	}

}

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

package com.alibaba.cloud.ai.example.graph.bigtool.agent;

import com.alibaba.cloud.ai.example.graph.bigtool.service.VectorStoreService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.HIT_TOOL;
import static com.alibaba.cloud.ai.example.graph.bigtool.constants.Constant.TOOL_LIST;

public class ToolNode implements NodeAction {

	private static Logger logger = LoggerFactory.getLogger(ToolNode.class);

	private ChatClient chatClient;

	private String inputTextKey;

	private String inputText;

	public ToolNode(ChatClient chatClient, String inputTextKey) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
	}

	public ToolNode(ChatClient chatClient, String inputTextKey, List<Document> documents) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
	}

	private static final String TOOL_PROMPT_TEMPLATE = """
			Respond to the human as helpfully and accurately as possible. You have access to the following tools:
			{tools}
			Use a json blob to specify a tool by providing an action key (tool name) and an action_input key (tool input).
			Valid "action" values: "final answer" or {tool_names}
			Provide only one action per $json_blob, as shown:

			{
			"action": $tool_name,
			"action_input": $input
			}

			Follow this format:
			Question: input question to answer
			Thought: consider previous and subsequent steps
			Action:

			$json_blob

			Observation: action result
			... (repeat thought/action/observation n times)
			Thought: I know what to respond
			Action:

			{
			"action": "final answer",
			"action_input": "final response to human"
			}

			Begin! Reminder to always respond with a valid json blob of a single action. Use tools if necessary. Respond directly if appropriate. Format is action: `$json_blob` then observation
			""";

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}
		logger.info("apply, inputText: {}", inputText);

		ChatResponse response = chatClient.prompt()
			.system(TOOL_PROMPT_TEMPLATE)
			.user(inputText)
			.tools(HIT_TOOL)
			.call()
			.chatResponse();

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("nextAction", "");
		if (state.value(inputTextKey).isPresent()) {
			updatedState.put(inputTextKey, response.getResult().getOutput().getText());
		}

		return updatedState;
	}

}

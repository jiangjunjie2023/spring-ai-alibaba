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
package com.alibaba.cloud.ai.example.graph.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(AgentNode.class);

	private static final String PROMPT_TEMPLATE_2 = """
			用户以工作流的方式处理任务，处理过程中有以下工具可以使用：
			{tools}

			要求尽可能以有效且准确的方式回应用户。以思维链的方式来决策工具的使用，遵循以下格式：
			问题: 待回答的输入问题
			思考: 考虑需要执行的操作
			操作: 下一步行动
			观察: 观察操作的结果

			... (可以重复n次 思考/曹组/观察)

			使用 JSON 块来指定工具，其中action代表下一步行动，有效的action值为："finalAnswer" 或者是 指定的工具名。
			1. 当思考得到最终回复结果时，输出action为finalAnswer，示例如下：
			{
				"thought": "任务已完成",
				"action": "finalAnswer",
				"output": "回复结果"
			}

			2. 当思考未得到最终回复结果时，输出要执行的工具，示例如下：
			{
				"thought": "考虑需要执行的操作",
				"action": "xuban_check",
				"params": {
			   	    "teacherCode": "",
			        "schoolId": "",
			        "classCode": ""
				}
			}

			开始！请记住每个步骤都要包含思考、操作和观察三个部分，并以JSON描述好下一步行动进行响应。
			""";

	private static final String PROMPT_TEMPLATE = """
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

	private SystemPromptTemplate systemPromptTemplate;

	private ChatClient chatClient;

	private String inputText;

	private List<String> categories;

	private List<String> classificationInstructions;

	private String inputTextKey;

	public AgentNode(ChatClient chatClient, String inputTextKey, List<String> categories,
			List<String> classificationInstructions) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
		this.categories = categories;
		this.classificationInstructions = classificationInstructions;
		this.systemPromptTemplate = new SystemPromptTemplate(PROMPT_TEMPLATE_2);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}

		List<Message> messages = new ArrayList<>();

		ChatResponse response = chatClient.prompt()
			.system(systemPromptTemplate.render(Map.of("inputText", inputText, "categories", categories,
					"classificationInstructions", classificationInstructions)))
			.user(inputText)
			.messages(messages)
			.call()
			.chatResponse();

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("classifier_output", response.getResult().getOutput().getText());
		if (state.value("messages").isPresent()) {
			updatedState.put("messages", response.getResult().getOutput());
		}

		return updatedState;
	}

	public static QuestionClassifierNode.Builder builder() {
		return new QuestionClassifierNode.Builder();
	}

	public static class Builder {

		private String inputTextKey;

		private ChatClient chatClient;

		private List<String> categories;

		private List<String> classificationInstructions;

		public AgentNode.Builder inputTextKey(String input) {
			this.inputTextKey = input;
			return this;
		}

		public AgentNode.Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public AgentNode.Builder categories(List<String> categories) {
			this.categories = categories;
			return this;
		}

		public AgentNode.Builder classificationInstructions(List<String> classificationInstructions) {
			this.classificationInstructions = classificationInstructions;
			return this;
		}

		public AgentNode build() {
			return new AgentNode(chatClient, inputTextKey, categories, classificationInstructions);
		}

	}

}

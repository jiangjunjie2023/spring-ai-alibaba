/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.graph.workflow;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class WorkflowAutoconfiguration {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowAutoconfiguration.class);

	private static final String NODE1 = "agent";

	private static final String NODE2 = "action";

	@Bean
	public StateGraph workflowGraph(ChatModel chatModel) throws GraphStateException {

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("question_handle_output", new ReplaceStrategy());
			state.registerKeyAndStrategy("solution", new ReplaceStrategy());
			return state;
		};

		QuestionClassifierNode feedbackClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.categories(List.of("positive feedback", "negative feedback"))
			.classificationInstructions(
					List.of("Try to understand the user's feeling when he/she is giving the feedback."))
			.build();

		QuestionClassifierNode specificQuestionClassifier = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.categories(List.of("after-sale service", "transportation", "product quality", "others"))
			.classificationInstructions(List
				.of("What kind of service or help the customer is trying to get from us? Classify the question based on your understanding."))
			.build();

		StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", stateFactory)
			.addNode(NODE1, node_async(feedbackClassifier))
			.addNode(NODE2, node_async(specificQuestionClassifier))

			.addEdge(START, NODE1)
			.addConditionalEdges(NODE1, edge_async(new CustomerServiceController.FeedbackQuestionDispatcher()),
					Map.of("continue", NODE2, "end", END));

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		System.out.println("\n\n");
		System.out.println(graphRepresentation.content());
		System.out.println("\n\n");

		return stateGraph;
	}

	private String runAgent() {
		String agentOutcome = "";// agent_runnable.invoke(data);
		logger.info("runAgent, " + agentOutcome);
		return agentOutcome;
	}

	// Define the function to execute tools
	private Map executeTools(Map data) {
		// Get the most recent agent_outcome - this is the key added in the `agent` above
		String agentAction = (String) data.get("agent_outcome");
		String output = "";// toolExecutor.invoke(agentAction);
		Map<String, Object> updatedState = new HashMap<>();
		Map<String, Object> actionMap = new HashMap<>();
		actionMap.put("action", agentAction);
		updatedState.put("intermediate_steps", actionMap);
		return updatedState;
	}

	private String shouldContinue(Map<String, Object> data) {
		logger.info("shouldContinue");
		// If the agent outcome is an AgentFinish, then we return `exit` string
		// This will be used when setting up the graph to define the flow
		String output = (String) data.get("output");
		if ("AgentFinish".equals(output)) {
			logger.info("end");
			finishAgent(data);
			return "end";
		}
		// Otherwise, an AgentAction is returned
		// Here we return `continue` string
		// This will be used when setting up the graph to define the flow
		else {
			logger.info("continue");
			return "continue";
		}
	}

	private void finishAgent(Map<String, Object> data) {
		// If the agent outcome is an AgentFinish, then we return `exit`string
		// This will be used when setting up the graph to define the flow
		String output = (String) data.get("output");
		if ("AgentFinish".equals(output)) {
			String result = (String) data.get("agentOutcome");
			logger.info("finish and result:" + result);
		}
		else {
			logger.info("'error'");
		}
	}

}

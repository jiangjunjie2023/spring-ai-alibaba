package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Configuration
public class AppAgentWorkflowConfig {

	@Bean("appAgentGraph")
	public StateGraph appAgentGraph(ChatModel chatModel) throws GraphStateException {
		ChatClient chatClient = ChatClient.builder(chatModel).build();

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("agent_outcome", new ReplaceStrategy());
			state.registerKeyAndStrategy("is_finish", new ReplaceStrategy());
			state.registerKeyAndStrategy("chat_history", new ReplaceStrategy());
			state.registerKeyAndStrategy("observation", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_output", new ReplaceStrategy());
			return state;
		};

		StateGraph stateGraph = new StateGraph("App Agent Workflow", stateFactory)
			.addNode("agent", AsyncNodeAction.node_async(new AgentNode(chatClient)))
			.addNode("action", AsyncNodeAction.node_async(new ActionNode()))
			.addNode("finish", AsyncNodeAction.node_async(new FinishNode()))
			.addEdge(START, "agent")
			.addConditionalEdges("agent", AsyncEdgeAction.edge_async(new ShouldContinueDispatcher()),
					Map.of("continue", "action", "finish", "finish"))
			.addEdge("action", "agent")
			.addEdge("finish", END);

		return stateGraph;
	}

}

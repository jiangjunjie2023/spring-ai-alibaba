package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.example.graph.node.*;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Configuration
public class AppAgentWorkflowConfig {

	private static final Logger log = LoggerFactory.getLogger(AppAgentWorkflowConfig.class);

	@Resource
	@Qualifier("myChatModel")
	private ChatModel chatModel;

	@Bean("appAgentGraph")
	public StateGraph appAgentGraph() throws GraphStateException {
		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("agent_outcome", new ReplaceStrategy());
			state.registerKeyAndStrategy("is_finish", new ReplaceStrategy());
			state.registerKeyAndStrategy("chat_history", new ReplaceStrategy());
			state.registerKeyAndStrategy("observation", new AppendStrategy());
			state.registerKeyAndStrategy("memory", new ReplaceStrategy());
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

	public StateGraph getAppGraphV2() throws GraphStateException {

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("output", new AppendStrategy());
			state.registerKeyAndStrategy("chat_history", new ReplaceStrategy());
			state.registerKeyAndStrategy("memory", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_output", new ReplaceStrategy());
			return state;
		};

		return new StateGraph("App Workflow V2", stateFactory)
			.addNode("start", AsyncNodeAction.node_async(new StartNode()))
			.addNode("node1", AsyncNodeAction.node_async(new Node1()))
			.addNode("node2", AsyncNodeAction.node_async(new Node2()))
			.addNode("finish", AsyncNodeAction.node_async(new ResultNode()))
			.addEdge(START, "start")
			.addEdge("start", "node1")
			.addEdge("start", "node2")
			.addEdge("node1", "finish")
			.addEdge("node2", "finish")
			.addEdge("finish", END);
	}

	@Bean
	public StateGraph queryWithHuman() throws GraphStateException {

		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			// 条件边控制：跳转下一个节点
			state.registerKeyAndStrategy("human_next_node", new ReplaceStrategy());
			// 用户输入
			state.registerKeyAndStrategy("query", new ReplaceStrategy());
			state.registerKeyAndStrategy("max_plan_iterations", new ReplaceStrategy());
			state.registerKeyAndStrategy("max_step_num", new ReplaceStrategy());
			state.registerKeyAndStrategy("classCnt", new ReplaceStrategy());

			state.registerKeyAndStrategy("feedback", new ReplaceStrategy());

			// 节点输出
			state.registerKeyAndStrategy("output", new ReplaceStrategy());
			state.registerKeyAndStrategy("plan_iterations", new ReplaceStrategy());
			state.registerKeyAndStrategy("observations", new ReplaceStrategy());
			state.registerKeyAndStrategy("final_report", new ReplaceStrategy());
			return state;
		};

		StateGraph stateGraph = new StateGraph("query with human", stateFactory)
			.addNode("start", AsyncNodeAction.node_async(new StartNode()))
			.addNode("node1", AsyncNodeAction.node_async(new Node1()))
			.addNode("human_feedback", AsyncNodeAction.node_async(new HumanFeedbackNode()))
			.addNode("node2", AsyncNodeAction.node_async(new Node2()))
			.addNode("finish", AsyncNodeAction.node_async(new ResultNode()))
			.addEdge(START, "start")
			.addEdge("start", "node1")
			.addEdge("node1", "human_feedback")
			.addConditionalEdges("human_feedback", AsyncEdgeAction.edge_async(new HumanFeedbackDispatcher()),
					Map.of("node2", "node2", "finish", "finish"))
			.addEdge("node2", END)
			.addEdge("finish", END);

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.MERMAID,
				"workflow graph");

		log.info("\n\n");
		log.info(graphRepresentation.content());
		log.info("\n\n");

		return stateGraph;
	}

}

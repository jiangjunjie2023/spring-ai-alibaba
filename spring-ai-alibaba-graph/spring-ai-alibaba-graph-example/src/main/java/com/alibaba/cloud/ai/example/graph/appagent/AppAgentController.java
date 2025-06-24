package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.fastjson.JSON;
import org.bsc.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/appagent")
public class AppAgentController {

	private static final Logger logger = LoggerFactory.getLogger(AppAgentController.class);

	private CompiledGraph compiledGraph;

	public AppAgentController(@Qualifier("appAgentGraph") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
	}

	@GetMapping("/chat")
	public String chat(String input) throws GraphStateException {
		// input: 教师周昌华想查看班级8WLHS2607下的学生报名情况
		input += "; 已知该老师email=zhouchanghua@xdf.cn，e2e=21D9AAB155F13810995FD41F3088F4A1，e2mf=d97fad3264654f8489388c23b60d74d4";
		logger.info("收到请求，input={}", input);
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", input));
		String output = result.map(state -> state.value("final_output").orElse("无结果").toString()).orElse("无结果");
		logger.info("返回结果：{}", output);
		return output;
	}

	@Autowired
	AppAgentWorkflowConfig appAgentWorkflowConfig;

	/**
	 * SSE (Server-Sent Events) endpoint for chat streaming.
	 *
	 * Accepts a ChatRequest and returns a Flux that streams chat responses as
	 * ServerSentEvent<String>. Supports both initial questions and human feedback
	 * handling.
	 */
	@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestBody(required = false) MyRequest chatRequest)
			throws GraphStateException {
		logger.info("chatStream, 收到请求，chatRequest={}", chatRequest);
		chatRequest = getDefaultChatRequest(chatRequest);

		Map<String, Object> objectMap = new HashMap<>();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
		compiledGraph = appAgentWorkflowConfig.queryWithHuman()
			.compile(CompileConfig.builder().saverConfig(saverConfig).interruptBefore("human_feedback").build());
		// Handle human feedback if auto-accept is disabled and feedback is provided
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(chatRequest.threadId()).build();
		if (!chatRequest.autoAcceptPlan() && StringUtils.hasText(chatRequest.interruptFeedback())) {
			handleHumanFeedback(chatRequest, objectMap, runnableConfig, sink);
		}
		// First question
		else {
			initializeObjectMap(chatRequest, objectMap);
			logger.info("init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);
			processStream(resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

	@PostMapping("/chat/resume")
	public Map<String, Object> resume(@RequestBody(required = false) FeedbackRequest humanFeedback) {
		logger.info("resume, 收到请求，humanFeedback={}", humanFeedback);
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(humanFeedback.threadId()).build();
		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feedback", humanFeedback.feedBack());
		objectMap.put("feed_back_content", humanFeedback.feedBackContent());

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "node2"));

		var resultFuture = compiledGraph.invoke(state, runnableConfig);
		return resultFuture.get().data();
	}

	@GetMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> resume(@RequestParam(value = "threadid") String threadId,
												@RequestParam(value = "feedback") boolean feedBack)
			throws GraphRunnerException {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
		StateSnapshot stateSnapshot = this.compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();

		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feedback", feedBack);

		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, ""));

		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		processStream(resultFuture, sink);

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

	@GetMapping("/chatv2")
	public String chatV2(String input) throws GraphStateException {
		logger.info("收到请求，input={}", input);
		CompiledGraph compiledGraph = appAgentWorkflowConfig.getAppGraphV2().compile();
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", input));
		String output = result.map(state -> state.value("final_output").orElse("无结果").toString()).orElse("无结果");
		logger.info("返回结果：{}", output);
		return output;
	}

	/**
	 * Creates a default ChatRequest instance or set some default value for an instance.
	 */
	private static MyRequest getDefaultChatRequest(MyRequest chatRequest) {
		if (chatRequest == null) {
			return new MyRequest("__default__", 1, 3, true, null, "草莓蛋糕怎么做呀。");
		}
		else {
			return new MyRequest(StringUtils.hasText(chatRequest.threadId()) ? chatRequest.threadId() : "__default__",
					chatRequest.maxPlanIterations() == null ? 1 : chatRequest.maxPlanIterations(),
					chatRequest.maxStepNum() == null ? 3 : chatRequest.maxStepNum(),
					chatRequest.autoAcceptPlan() == null || chatRequest.autoAcceptPlan(),
					chatRequest.interruptFeedback(),
					StringUtils.hasText(chatRequest.query()) ? chatRequest.query() : "草莓蛋糕怎么做呀");
		}
	}

	private static void initializeObjectMap(MyRequest chatRequest, Map<String, Object> objectMap) {
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("auto_accepted_plan", chatRequest.autoAcceptPlan());
		objectMap.put("query", chatRequest.query());
		objectMap.put("max_step_num", chatRequest.maxStepNum());
		objectMap.put("max_plan_iterations", chatRequest.maxPlanIterations());
	}

	public void handleHumanFeedback(MyRequest chatRequest, Map<String, Object> objectMap, RunnableConfig runnableConfig,
			Sinks.Many<ServerSentEvent<String>> sink) {
		objectMap.put("feed_back", chatRequest.interruptFeedback());
		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "node2"));
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		processStream(resultFuture, sink);
	}

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public void processStream(AsyncGenerator<NodeOutput> generator, Sinks.Many<ServerSentEvent<String>> sink) {
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					Map<String, Object> data = output.state().data();
					sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(data)).build());
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				// 正常完成
				sink.tryEmitComplete();
			}).exceptionally(e -> {
				sink.tryEmitError(e);
				return null;
			});
		});
	}

}

package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;
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
	 * Accepts a ChatRequest and returns a Flux that streams chat responses as
	 * ServerSentEvent<String>. Supports both initial questions and human feedback
	 * handling.
	 */
	@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStream(@RequestBody MyRequest chatRequest) throws GraphStateException {
		logger.info("chatStream, 收到请求，chatRequest={}", chatRequest);
		Map<String, Object> objectMap = new HashMap<>();
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(chatRequest.threadId()).build();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		if (StringUtils.hasText(chatRequest.feedback())) {
			objectMap.put("feedback", chatRequest.feedback());

			// 恢复工作流
			StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
			OverAllState state = stateSnapshot.state();
			state.withResume();
			state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "human_feedback"));
			// 重启工作流
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
			processStream(resultFuture, sink);
		}
		// 初始问题, 首次启动工作流
		else {
			// 构建可恢复的工作流 设定某节点需要人类的反馈信息
			SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
			compiledGraph = appAgentWorkflowConfig.queryWithHuman()
				.compile(CompileConfig.builder().saverConfig(saverConfig).interruptBefore("human_feedback").build());//

			initializeObjectMap(chatRequest, objectMap);
			logger.info("init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);
			processStream(resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("chatStream, Client disconnected from stream"))
			.doOnError(e -> logger.error("chatStream, Error occurred during streaming", e));
	}

	/**
	 * 恢复流式对话
	 */
	@PostMapping(value = "/chat/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> resume(@RequestBody FeedbackRequest humanFeedback) {
		logger.info("resume, 收到请求，humanFeedback={}", humanFeedback);
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(humanFeedback.threadId()).build();
		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feedback", humanFeedback.feedBack());

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, ""));

		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		processStream(resultFuture, sink);

		return sink.asFlux()
			.doOnCancel(() -> logger.info("resume, Client disconnected from stream"))
			.doOnError(e -> logger.error("resume, Error occurred during streaming", e));
	}

	/**
	 * 固定工作流
	 */
	@GetMapping("/chatv2")
	public String chatV2(String input) throws GraphStateException {
		logger.info("收到请求，input={}", input);
		CompiledGraph compiledGraph = appAgentWorkflowConfig.getAppGraphV2().compile();
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", input));
		String output = result.map(state -> state.value("final_output").orElse("无结果").toString()).orElse("无结果");
		logger.info("返回结果：{}", output);
		return output;
	}

	@PostMapping(value = "/chat/xuban", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatXuban(@RequestBody CommonRequest chatRequest) throws GraphStateException {
		logger.info("chatXuban, 收到请求，chatRequest={}", chatRequest);
		Map<String, Object> objectMap = new HashMap<>();
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(chatRequest.threadId()).build();
		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		if (StringUtils.hasText(chatRequest.feedback())) {
			initParams(chatRequest, objectMap);
			logger.info("chatXuban, resume inputs: {}", objectMap);

			// 恢复工作流
			StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
			OverAllState state = stateSnapshot.state();
			state.withResume();
			state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "node1"));
			// 重启工作流
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
			processStream(resultFuture, sink);
		} else {
			// 初始问题, 首次启动工作流
			// 构建可恢复的工作流 设定某节点需要人类的反馈信息
			SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
			compiledGraph = appAgentWorkflowConfig.xubanFlow()
				.compile(CompileConfig.builder().saverConfig(saverConfig).build());//.interruptBefore("node2")

			initParams(chatRequest, objectMap);
			logger.info("chatXuban, init inputs: {}", objectMap);
			AsyncGenerator<NodeOutput> resultFuture = compiledGraph.stream(objectMap, runnableConfig);

			processStream(resultFuture, sink);
		}

		return sink.asFlux()
			.doOnCancel(() -> logger.info("chatStream, Client disconnected from stream"))
			.doOnError(e -> logger.error("chatStream, Error occurred during streaming", e));
	}

	private static void initializeObjectMap(MyRequest chatRequest, Map<String, Object> objectMap) {
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("query", chatRequest.query());
		objectMap.put("max_plan_iterations", chatRequest.maxPlanIterations());
		objectMap.put("max_step_num", chatRequest.maxStepNum());
		objectMap.put("feedback", chatRequest.feedback());
	}

	private static void initParams(CommonRequest chatRequest, Map<String, Object> objectMap) {
		objectMap.put("thread_id", chatRequest.threadId());
		objectMap.put("input", chatRequest.input());
		objectMap.put("feedback", chatRequest.feedback());
	}

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public void processStream(AsyncGenerator<NodeOutput> generator, Sinks.Many<ServerSentEvent<String>> sink) {
		executor.submit(() -> {
			generator.forEachAsync(output -> {
				try {
					Map<String, Object> data = output.state().data();
					String json = JSON.toJSONString(data);
					logger.info("processStream, json={}", json);
					sink.tryEmitNext(ServerSentEvent.builder(json).build());
				}
				catch (Exception e) {
					logger.error("processStream, error", e);
					throw new CompletionException(e);
				}
			}).thenAccept(v -> {
				logger.info("processStream, complete");
				// 正常完成
				sink.tryEmitComplete();
			}).exceptionally(e -> {
				logger.error("processStream error", e);
				// 递归查找 GraphInterruptException
				Throwable cause = e;
				while (cause != null) {
					if (cause instanceof GraphInterruptException) {
						GraphInterruptException gie = (GraphInterruptException) cause;
						// 构造中断提示消息
						Map<String, Object> interruptMsg = new HashMap<>();
						interruptMsg.put("interrupt", true);
						interruptMsg.put("interrupt_tip", gie.getMessage());
						String json = JSON.toJSONString(interruptMsg);
						logger.info("processStream, interrupt: {}", json);
						sink.tryEmitNext(ServerSentEvent.builder(json).build());
						sink.tryEmitComplete();
						return null;
					}
					cause = cause.getCause();
				}
				sink.tryEmitError(e);
				return null;
			});
		});
	}

}

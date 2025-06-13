package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/appagent")
public class AppAgentController {

	private static final Logger logger = LoggerFactory.getLogger(AppAgentController.class);

	private final CompiledGraph compiledGraph;

	public AppAgentController(@Qualifier("appAgentGraph") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
	}

	@GetMapping("/chat")
	public String chat(String input) throws GraphStateException {
		input += "; 已知该老师email=yueqianqian@xdf.cn，e2e=894DC59F52530720C11F2140AFC8F701，e2mf=f9019ccb148e4e98acf71fbcc09b4958";
		logger.info("收到请求，input={}", input);
		Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", input));
		String output = result.map(state -> state.value("final_output").orElse("无结果").toString()).orElse("无结果");
		logger.info("返回结果：{}", output);
		return output;
	}

}

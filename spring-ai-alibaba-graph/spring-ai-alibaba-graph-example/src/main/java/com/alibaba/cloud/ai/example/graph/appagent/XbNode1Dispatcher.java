package com.alibaba.cloud.ai.example.graph.appagent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class XbNode1Dispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(XbNode1Dispatcher.class);

	@Override
	public String apply(OverAllState state) {
		logger.info("XbNode1Dispatcher，state={}", state.data());
		String nextNode = "node2";
		Boolean interrupt = (Boolean) state.value("interrupt").orElse(false);
		logger.info("XbNode1Dispatcher，interrupt={}", interrupt);
		String interruptTip = (String) state.value("interrupt_tip").orElse("");
		logger.info("XbNode1Dispatcher，interruptTip={}", interruptTip);
		// List observationList = state.value("observation", List.class).orElse(new
		// ArrayList<>());
		String observation = state.value("observation").orElse("").toString();
		logger.info("XbNode1Dispatcher，observation={}", observation);
		if (interrupt) {
			nextNode = "human_feedback";
		}
		return nextNode;
	}

}

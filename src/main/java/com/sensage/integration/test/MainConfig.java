package com.sensage.integration.test;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

@Configuration
public class MainConfig {

	@Autowired
	private IntegrationFlowContext intContext;

	@Value("${readingDir}")
	private String readingDir;

	@PostConstruct
	private void initDispatchers() throws Exception {
		FileDispatcher f = new FileDispatcher(readingDir);
		f.initDispatcher(intContext);
	}
}

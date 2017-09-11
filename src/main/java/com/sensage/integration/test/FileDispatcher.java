package com.sensage.integration.test;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.DirectChannelSpec;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileReadingMessageSource.WatchEventType;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;

@EnableIntegration
public class FileDispatcher {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final String readingDir;

	public FileDispatcher(String readindDir) {
		this.readingDir = readindDir;
	}

	public void initDispatcher(IntegrationFlowContext intContext) {

		DirectChannelSpec output = MessageChannels.direct("output");

		IntegrationFlow fout = IntegrationFlows.from(output)
				 .filter(m -> m.toString().trim().length() != 0)
				 .handle(m -> log.info(m.getPayload().toString()))
				 .get();

		IntegrationFlow fin = IntegrationFlows
				.from(this.getFileMessageSource(), s -> s.poller(Pollers.fixedRate(100, TimeUnit.MILLISECONDS)))
				.channel(new DirectChannel())
				.handle(m -> {
					File f = (File) m.getPayload();

					String id = "tailer-" + f.getAbsolutePath();
					IntegrationFlowRegistration bean = intContext.getRegistrationById(id);
					if(bean != null) {
						bean.stop();
						intContext.remove(id);
					}

					IntegrationFlow fileFlow = IntegrationFlows.from(getTailer(f)).channel(output).get();
					intContext.registration(fileFlow).id(id).register();
				}).get();

		intContext.registration(fout).id("fout").register();
		intContext.registration(fin).id("fin").autoStartup(true).register();
	}

	private FileTailingMessageProducerSupport getTailer(File f) {
		ApacheCommonsFileTailingMessageProducer tailer = new ApacheCommonsFileTailingMessageProducer();
		tailer.setFile(f);
		tailer.setEnd(false);
		tailer.setReopen(true);
		
		return tailer;
	}
	
	private FileReadingMessageSource getFileMessageSource() {
		FileReadingMessageSource lm = new FileReadingMessageSource();
		lm.setDirectory(new File(readingDir));
		lm.setUseWatchService(true);
		lm.setWatchEvents(WatchEventType.CREATE);

		//TODO: Improve filter with globbing parsing: http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29
		lm.setFilter(
				l -> Stream.of(l).filter(f -> f.getAbsolutePath().matches("^.*\\.txt$")).collect(Collectors.toList()));

		return lm;
	}
}

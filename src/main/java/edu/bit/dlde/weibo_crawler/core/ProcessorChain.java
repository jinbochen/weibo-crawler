package edu.bit.dlde.weibo_crawler.core;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 不同与wks的mirrorengine，本作优化了各个processor之间串行处理
 * @author lins
 * @date 2012-6-19
 **/
public class ProcessorChain extends LinkedList<Consumer<?>> {
	private static final long serialVersionUID = 3520248140430417357L;
	private static final Logger logger = LoggerFactory
			.getLogger(ProcessorChain.class);

	public void process() throws Exception{
		for (Consumer<?> c : this) {
			logger.debug("{} begin to consume data from {}", c.getClass()
					.toString().replaceAll(".*\\.", ""), c.getProducer()
					.getClass().toString().replaceAll(".*\\.", ""));
			c.consume();
			logger.debug("{} complete consuming data from {}", c.getClass()
					.toString().replaceAll(".*\\.", ""), c.getProducer()
					.getClass().toString().replaceAll(".*\\.", ""));
		}
	}

}

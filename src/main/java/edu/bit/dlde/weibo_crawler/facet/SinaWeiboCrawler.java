package edu.bit.dlde.weibo_crawler.facet;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sina weibo爬虫
 * 
 * @author lins
 * @date 2012-6-18
 **/
public class SinaWeiboCrawler implements WeiboCrawler {
	private static final Logger logger = LoggerFactory
			.getLogger(SinaWeiboCrawler.class);

	public void run() {
		logger.info("{} begins at {}.", this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		
	}

	public boolean stop() {
		logger.info("{} shuts down at {}.", this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		return true;
	}

	public static void main(String[] args) {
		SinaWeiboCrawler sinaWeiboCrawler = new SinaWeiboCrawler();
		sinaWeiboCrawler.stop();
	}
}

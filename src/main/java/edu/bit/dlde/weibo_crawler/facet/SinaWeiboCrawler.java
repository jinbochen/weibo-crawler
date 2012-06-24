package edu.bit.dlde.weibo_crawler.facet;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bit.dlde.weibo_crawler.core.Manager;

/**
 * sina weibo爬虫
 * 
 * @author lins
 * @date 2012-6-18
 **/
public class SinaWeiboCrawler implements WeiboCrawler {
	private static final Logger logger = LoggerFactory
			.getLogger(SinaWeiboCrawler.class);
	private Manager manager = new Manager();
	
	public void run() {
		logger.info("{} began at {}.", this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		manager.init();
	}

	public boolean stop() {
		logger.info("{} shut down at {}.", this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		return true;
	}

	public boolean pause(){
		logger.info("{} pasued at {}.", this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		return true;
	}
	
	public static void main(String[] args) {
		SinaWeiboCrawler sinaWeiboCrawler = new SinaWeiboCrawler();
		sinaWeiboCrawler.stop();
	}
}

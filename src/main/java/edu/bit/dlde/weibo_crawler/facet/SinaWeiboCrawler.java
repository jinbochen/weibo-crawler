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

	// 0-stop; 1-run; 2-pause
	private int status = 0;

	public int getStatus() {
		return status;
	}

	/**
	 * 启动爬虫
	 * @see edu.bit.dlde.weibo_crawler.facet.WeiboCrawler#start()
	 */
	public boolean start() {
		logger.info("{} began at {}.",
				this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		if (status == 0) {
			manager.init();
			if (manager.isLoadWithoutExceptions())
				status = 1;
			else
				logger.error("{} failed to init at {}.", this.getClass()
						.toString().replaceAll(".*\\.", ""),
						new Date().toString());
		} else {
			logger.info("{} was running already.", this.getClass().toString()
					.replaceAll(".*\\.", ""));
		}
		return status == 1;
	}

	/**
	 * 停止爬虫
	 * 
	 * @see edu.bit.dlde.weibo_crawler.facet.WeiboCrawler#stop()
	 */
	public boolean stop() {
		logger.info("{} shut down at {}.", this.getClass().toString()
				.replaceAll(".*\\.", ""), new Date().toString());
		if (status == 1) {
			manager.stop();
			if (manager.isStopWithoutExceptions())
				status = 0;
			else
				logger.error("{} failed to stop at {}.", this.getClass()
						.toString().replaceAll(".*\\.", ""),
						new Date().toString());
		} else {
			logger.info("{} was stopped already.", this.getClass().toString()
					.replaceAll(".*\\.", ""));
		}
		return status == 0;
	}

	/**
	 * 暂停爬虫
	 * 
	 * @see edu.bit.dlde.weibo_crawler.facet.WeiboCrawler#pause(long)
	 */
	public boolean pause(long millis) {
		logger.info("{} pasued at {}.",
				this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		if (status == 1) {
			manager.pause(millis);
			if (manager.isPauseWithoutExceptions())
				status = 2;
			else
				logger.error("{} failed to pause at {}.", this.getClass()
						.toString().replaceAll(".*\\.", ""),
						new Date().toString());
		} else {
			logger.info("{} was not running yet.", this.getClass().toString()
					.replaceAll(".*\\.", ""));
		}
		return status == 2;
	}

	/**
	 * 继续爬虫
	 * @see edu.bit.dlde.weibo_crawler.facet.WeiboCrawler#goon()
	 */
	public boolean goon() {
		logger.info("{} continues at {}.",
				this.getClass().toString().replaceAll(".*\\.", ""),
				new Date().toString());
		if (status == 2) {
			manager.goon();
			if (manager.isGoonWithoutExceptions())
				status = 1;
			else
				logger.error("{} failed to continue at {}.", this.getClass()
						.toString().replaceAll(".*\\.", ""),
						new Date().toString());
		} else {
			logger.info("{} has not been paused yet.", this.getClass().toString()
					.replaceAll(".*\\.", ""));
		}
		return status == 1;
	}

	public static void main(String[] args) {
		SinaWeiboCrawler sinaWeiboCrawler = new SinaWeiboCrawler();
		sinaWeiboCrawler.stop();
	}
}

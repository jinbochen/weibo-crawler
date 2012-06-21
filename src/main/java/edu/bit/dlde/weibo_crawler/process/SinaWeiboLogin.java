package edu.bit.dlde.weibo_crawler.process;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.data.Seed;

import edu.bit.dlde.weibo_crawler.auth.LoginFailureException;
import edu.bit.dlde.weibo_crawler.auth.SinaWeiboLoginAuth;
import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Provider;

/**
 * 处理登录问题，用以生成cookie
 * @author lins
 * @date 2012-6-19
 **/
public class SinaWeiboLogin implements Processor<String, Seed> {
	private final static Logger logger = LoggerFactory
			.getLogger(SinaWeiboLogin.class);
	Provider<Seed> provider;

	public void setProvider(Provider<Seed> p) {
		this.provider = p;
	}

	public Provider<Seed> getProvider() {
		return provider;
	}

	public void consume(Provider<Seed> p) throws Exception {
		this.provider = p;
		consume();
	}

	int sleep = 1;

	/**
	 * 消费掉Seed，用于生产
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Consumer#consume()
	 */
	public void consume() throws Exception {
		// 从生产者那边获得seed
		Seed seed;
		do {
			seed = provider.produce();
			if (seed != null) {
				sleep = 1;
				break;
			}

			Thread.sleep(sleep * 1000);

			if (sleep != 60)
				sleep++;
			else {
				// 长时间休眠则提醒provider,重新产生新的seed
				provider.notifyMyself();
				// 出于cookie过期的考虑重置cookies
				cookies = new LinkedBlockingQueue<String>();
			}
		} while (true);

		// 登录验证,用于生产
		SinaWeiboLoginAuth sinaLogin = new SinaWeiboLoginAuth(
				new DefaultHttpClient());
		sinaLogin.try2Login(seed.getAccount(), seed.getPassword());

		if (!cookies.contains(sinaLogin.getCookie()))
			cookies.add(sinaLogin.getCookie());
	}

	private volatile static LinkedBlockingQueue<String> cookies = new LinkedBlockingQueue<String>();

	/**
	 * 用于产生爬虫的cookies，等待时间为1s，否则中断，返回null
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#produce()
	 */
	public String produce() {
		String cookie;
		try {
			cookie = cookies.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("{} was inturrupted while trying to produce.", this
					.getClass().toString().replaceAll(".*\\.", ""));
			return null;
		}
		return cookie;
	}

	/**
	 * 无效的方法
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#produceMega()
	 */
	@Deprecated
	public Collection<String> produceMega() {
		return cookies;
	}

	/**
	 * 单线程已经足够处理seed
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.debug("{} start to consume.", this.getClass().toString()
				.replaceAll(".*\\.", ""));
		while (true) {
			try {
				consume();
			} catch (LoginFailureException e) {
				logger.debug("Fail to login.");
				continue;
			} catch (Exception e) {
				logger.debug("Unknow exception was caught.");
				continue;
			}
		}
	}

	/**
	 * 手动跑一次consume
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#notifyMyself()
	 */
	public void notifyMyself() {
		try {
			consume();
		} catch (LoginFailureException e) {
			logger.debug("Fail to login.");
		} catch (Exception e) {
			logger.debug("Unknow exception was caught.");
		}
	}

}

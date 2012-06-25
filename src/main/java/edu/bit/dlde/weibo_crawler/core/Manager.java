package edu.bit.dlde.weibo_crawler.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import bit.mirror.dao.mongo.MongoDao;

import edu.bit.dlde.weibo_crawler.process.RedundancyFilter;
import edu.bit.dlde.weibo_crawler.process.SeedProvider;
import edu.bit.dlde.weibo_crawler.process.WeiboFetcher;
import edu.bit.dlde.weibo_crawler.process.WeiboLogin;
import edu.bit.dlde.weibo_crawler.process.WeiboSaver;

/**
 * 管理所有的processor的一个类
 * 
 * @author lins
 * @date 2012-6-21
 **/
public class Manager {
	/*** 提供seed ***/
	public SeedProvider seedProvider;
	/*** 根据帐号获得登录cookie ***/
	public WeiboLogin weiboLogin;
	/*** 爬取微博 ***/
	public WeiboFetcher weiboFetcher;
	/*** 去重 ***/
	public RedundancyFilter redundancyFilter;
	/*** 入库 ***/
	public WeiboSaver weiboSaver;

	private boolean loadWithoutExceptions = false;
	private boolean pauseWithoutExceptions = false;
	private boolean stopWithoutExceptions = false;
	private boolean goonWithoutExceptions = false;

	MongoDao dao;

	public static ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors
			.newCachedThreadPool();

	public boolean init() {
		// dao.setMongo(mongo);
		if (dao == null) {
			dao = new MongoDao();
			dao.start();
		}
		loadWithoutExceptions = true;

		seedProvider = new SeedProvider();
		seedProvider.setDao(dao);
		seedProvider.setManager(this);

		weiboLogin = new WeiboLogin();
		weiboLogin.setManager(this);
		weiboLogin.setProducer(seedProvider);

		weiboFetcher = new WeiboFetcher();
		weiboFetcher.setManager(this);
		weiboFetcher.setProducer(weiboLogin);

		redundancyFilter = new RedundancyFilter();
		redundancyFilter.setManager(this);
		redundancyFilter.setDao(dao);
		redundancyFilter.setProducer(weiboFetcher);

		weiboSaver = new WeiboSaver();
		weiboSaver.setDao(dao);
		weiboSaver.setManager(this);
		weiboSaver.setProducer(redundancyFilter);

		exec.execute(seedProvider);
		exec.execute(weiboLogin);
		exec.execute(weiboFetcher);
		exec.execute(redundancyFilter);
		exec.execute(weiboSaver);
		
		return loadWithoutExceptions;
	}

	public boolean stop() {
		stopWithoutExceptions = true;
		return stopWithoutExceptions;
	}

	public boolean pause(long millis) {
		pauseWithoutExceptions = true;
		return pauseWithoutExceptions;
	}

	public boolean goon() {
		goonWithoutExceptions = true;
		return goonWithoutExceptions;
	}

	public void fireCookieReload() {

	}

	public void fireFetcherReset() {

	}

	public boolean isLoadWithoutExceptions() {
		return loadWithoutExceptions;
	}

	public void setLoadWithoutExceptions(boolean loadWithoutExceptions) {
		this.loadWithoutExceptions = loadWithoutExceptions;
	}

	public boolean isPauseWithoutExceptions() {
		return pauseWithoutExceptions;
	}

	public void setPauseWithoutExceptions(boolean pauseWithoutExceptions) {
		this.pauseWithoutExceptions = pauseWithoutExceptions;
	}

	public boolean isStopWithoutExceptions() {
		return stopWithoutExceptions;
	}

	public void setStopWithoutExceptions(boolean stopWithoutExceptions) {
		this.stopWithoutExceptions = stopWithoutExceptions;
	}

	public boolean isGoonWithoutExceptions() {
		return goonWithoutExceptions;
	}

	public void setGoonWithoutExceptions(boolean goonWithoutExceptions) {
		this.goonWithoutExceptions = goonWithoutExceptions;
	}

}

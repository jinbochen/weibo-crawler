package edu.bit.dlde.weibo_crawler.core;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import bit.mirror.dao.mongo.MongoDao;

import edu.bit.dlde.weibo_crawler.process.RedundancyFilter;
import edu.bit.dlde.weibo_crawler.process.SeedProvider;
import edu.bit.dlde.weibo_crawler.process.Fetcher;
import edu.bit.dlde.weibo_crawler.process.Login;
import edu.bit.dlde.weibo_crawler.process.Saver;

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
	public Login weiboLogin;
	/*** 爬取微博 ***/
	public Fetcher weiboFetcher;
	/*** 去重 ***/
	public RedundancyFilter redundancyFilter;
	/*** 入库 ***/
	public Saver weiboSaver;
	/*** 各种标志位 ***/
	private boolean loadWithoutExceptions = false;
	private boolean pauseWithoutExceptions = false;
	private boolean stopWithoutExceptions = false;
	private boolean goonWithoutExceptions = false;
	/*** dao ***/
	MongoDao dao;
	/*** 整个crawler的线程池 ***/
	public static ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors
			.newCachedThreadPool();
	static {
		exec.setCorePoolSize(50);
		exec.setMaximumPoolSize(80);
	}

	public boolean init() {
		// dao.setMongo(mongo);
		if (dao == null) {
			Mongo mongo = null;
			try {
				mongo = new Mongo("10.1.0.171", 27017);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (MongoException e) {
				e.printStackTrace();
			}
			Morphia morphia = new Morphia();
			Datastore datastore = morphia.createDatastore(mongo, "genius");
			dao = new MongoDao();
			dao.setMongo(mongo);
			dao.setDatastore(datastore);
			dao.start();
		}
		loadWithoutExceptions = true;

		seedProvider = new SeedProvider();
		seedProvider.setDao(dao);
		seedProvider.setManager(this);

		weiboLogin = new Login();
		weiboLogin.setManager(this);
		weiboLogin.setProducer(seedProvider);

		weiboFetcher = new Fetcher();
		weiboFetcher.setManager(this);
		weiboFetcher.setProducer(weiboLogin);

		redundancyFilter = new RedundancyFilter();
		redundancyFilter.setManager(this);
		redundancyFilter.setDao(dao);
		redundancyFilter.setProducer(weiboFetcher);

		weiboSaver = new Saver();
		weiboSaver.setDao(dao);
		weiboSaver.setManager(this);
		weiboSaver.setProducer(redundancyFilter);

		exec.execute(seedProvider);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		exec.execute(weiboLogin);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		exec.execute(weiboFetcher);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		exec.execute(redundancyFilter);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
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

	public void fireSeedReloadEvent() {
		
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

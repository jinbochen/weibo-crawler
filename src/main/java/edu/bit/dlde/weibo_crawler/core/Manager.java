package edu.bit.dlde.weibo_crawler.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import edu.bit.dlde.weibo_crawler.process.SeedProvider;
import edu.bit.dlde.weibo_crawler.process.WeiboFetcher;
import edu.bit.dlde.weibo_crawler.process.WeiboLogin;

/**
 *管理所有的processor的一个类
 *@author lins
 *@date 2012-6-21
 **/
public class Manager {
	public SeedProvider seedProvider;
	public WeiboLogin weiboLogin;
	public WeiboFetcher weiboFetcher;

	public static ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors
			.newCachedThreadPool();
	public void init(){
		
	}
	
	public boolean stop(){
		return true;
	}
	
	public boolean pause(){
		return true;
	}
	
	public void reloadCookie() {
		
	}

}

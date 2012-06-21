package edu.bit.dlde.weibo_crawler.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Provider;
import edu.bit.dlde.weibo_crawler.utils.StreamFormator;

/**
 * 抓取微博的类获得15条微博，将获得结果传送给下一个processor， 并且通过notifyMyself提醒自己找到重复的了
 * 
 * @author lins
 * @date 2012-6-20
 **/
public class SinaWeiboFetcher implements Processor<JSONObject, String> {
	private static final Logger logger = LoggerFactory
			.getLogger(SinaWeiboFetcher.class);

	Provider<String> producer;

	public void setProvider(Provider<String> p) {
		this.producer = p;
	}

	public Provider<String> getProvider() {
		return producer;
	}

	private volatile static LinkedBlockingQueue<JSONObject> jsonObjs = new LinkedBlockingQueue<JSONObject>();

	public JSONObject produce() {
		return jsonObjs.poll();
	}

	public Collection<JSONObject> produceMega() {
		return jsonObjs;
	}

	public void consume(Provider<String> p) throws Exception {
		this.producer = p;
		consume();
	}

	private static ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors
			.newCachedThreadPool();
	private volatile ArrayList<Runnable> runnables = new ArrayList<Runnable>();
	int threadCount = 10;

	public void consume() throws Exception {
		if (producer == null) {
			logger.debug("Provider of weibo login unavilable.");
			return;
		}
		String cookie = null;

		for (cookie = producer.produce(); true; cookie = producer.produce()) {
			if (cookie == null || cookie.equals("")) {
				Thread.sleep(1000);
			}
			// 默认每个cookie开10条线进行爬虫
			for (int i = 0; i < threadCount; i++) {
				// 真正在爬微博的线程
				Runnable r = new FetchThread(i, cookie);
				exec.execute(r);
				runnables.add(r);
			}
		}
	}

	/*
	 * 只有在跑起来之前设置才有效
	 */
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public void notifyMyself() {

	}

	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.error("Error in fetching weibo!");
		}
	}

	private String ajaxUrl = "http://weibo.com/aj/mblog/fsearch";

	public String getAjaxUrl() {
		return ajaxUrl;
	}

	public void setAjaxUrl(String ajaxUrl) {
		this.ajaxUrl = ajaxUrl;
	}

	/**
	 * 真正用来爬虫的线程
	 */
	public class FetchThread implements Runnable {
		private int id;
		private String cookie;

		public FetchThread(int id, String cookie) {
			this.id = id;
			this.cookie = cookie;
		}

		public int getId() {
			return id;
		}

		public String getCookie() {
			return cookie;
		}

		/**
		 * 一个线程保持一个HttpClient，不停地发送请求，获得微薄
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			HttpClient httpClient = new DefaultHttpClient();
			while (true) {

				HttpGet get = new HttpGet(ajaxUrl);
				get.addHeader("Cookie", cookie);
				HttpResponse hr;
				try {
					hr = httpClient.execute(get);
					HttpEntity httpEntity = hr.getEntity();
					InputStream inputStream = httpEntity.getContent();
					String tmp = StreamFormator.getString(inputStream, "gbk");
					logger.debug(tmp);
					JSONObject jsonObj = JSONObject.fromObject(tmp);
					jsonObj.accumulate("thread-id", id);
					jsonObjs.add(jsonObj);
					inputStream.close();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}

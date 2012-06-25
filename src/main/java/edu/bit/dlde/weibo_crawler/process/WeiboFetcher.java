package edu.bit.dlde.weibo_crawler.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.data.Seed;

import edu.bit.dlde.weibo_crawler.core.Manager;
import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Producer;
import edu.bit.dlde.weibo_crawler.utils.StreamFormator;

/**
 * 抓取微博的类,获得15条微博，将获得结果传送给下一个processor
 * 
 * @author lins
 * @date 2012-6-20
 **/
public class WeiboFetcher implements Processor<JSONObject, JSONObject> {
	private static final Logger logger = LoggerFactory
			.getLogger(WeiboFetcher.class);
	private Manager manager;
	volatile private Producer<JSONObject> producer;

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	public void setProducer(Producer<JSONObject> p) {
		this.producer = p;
	}

	public Producer<JSONObject> getProducer() {
		return producer;
	}

	private volatile static LinkedBlockingQueue<JSONObject> jsonObjs = new LinkedBlockingQueue<JSONObject>();

	public JSONObject produce() {
		return jsonObjs.poll();
	}

	public Collection<JSONObject> produceMega() {
		return jsonObjs;
	}

	public void consume(Producer<JSONObject> p) throws Exception {
		this.producer = p;
		consume();
	}

	/***由于这些线程是常驻的，所以把他们记录下来***/
	private volatile ArrayList<Runnable> runnables = new ArrayList<Runnable>();
	int threadCount = 10;

	public void consume() throws Exception {
		if (producer == null) {
			logger.debug("Provider of weibo login unavilable.");
			return;
		}
		JSONObject cookie = null;

		int sleep = 1;
		for (cookie = producer.produce(); true; cookie = producer.produce()) {
			if (cookie == null || cookie.equals("")) {
				logger.debug("Cookie for weibo login unavilable.");
				Thread.sleep(sleep * 1000);
				if (sleep < 60)
					sleep++;
				continue;
			}
			sleep = 1;
			// 默认每个cookie开10条线进行爬虫
			for (int i = 0; i < threadCount; i++) {
				// 真正在爬微博的线程
				Runnable r = new FetchThread(i, cookie);
				Manager.exec.execute(r);
				runnables.add(r);
			}
		}
	}

	/**
	 * 只有在跑起来之前设置才有效
	 */
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
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
		private JSONObject jsonObj;

		public FetchThread(int id, JSONObject cookie) {
			this.id = id;
			this.jsonObj = cookie;
			page = id + 1;
		}

		public int getId() {
			return id;
		}

		public JSONObject getCookie() {
			return jsonObj;
		}

		private int page;
		private int count = 15;
		private int pre_page = 1;
		private int pagebar = -1;

		/**
		 * 每个page的第一个都是纯粹的AjaxUrl，接着pagebar增从0-1，然后再增加page
		 */
		private String getNextAjaxUrl() {
			StringBuilder sb = new StringBuilder(ajaxUrl);
			if (pagebar != -1) {
				sb.append("?");
				sb.append("page=" + page);
				sb.append("&count=" + count);
				sb.append("&pre_page=" + pre_page);
				sb.append("&pagebar=" + pagebar);
				pre_page = page;
				// pagebar从0-1，page从id开始自增
				if (pagebar == 1) {
					page += threadCount;
					pagebar = -2;
				}
			}
			pagebar++;
			return sb.toString();
		}

		public void reset() {
			page = id + 1;
			count = 15;
			pre_page = 1;
			pagebar = -1;
		}

		/**
		 * 一个线程保持一个HttpClient，不停地发送请求，获得微薄
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			HttpClient httpClient = new DefaultHttpClient();
			while (true) {
				String uri = getNextAjaxUrl();
				HttpGet get = new HttpGet(uri);
				get.addHeader("Cookie", jsonObj.getString("cookie"));
				HttpResponse hr;
				try {
					hr = httpClient.execute(get);
					HttpEntity httpEntity = hr.getEntity();
					InputStream inputStream = httpEntity.getContent();
					String tmp = StreamFormator.getString(inputStream, "gbk");
					logger.debug(tmp);
					JSONObject jsonObj = JSONObject.fromObject(tmp);
					jsonObj.accumulate("thread-id", id);
					// 伪造的一个uri
					Seed seed = (Seed) jsonObj.get("seed");
					jsonObj.accumulate("uri",
							uri + "?date=" + new Date() + "?thread-id=" + id
									+ "?account=" + seed.getAccount());
					jsonObjs.add(jsonObj);
					inputStream.close();
				} catch (ClientProtocolException e) {
					logger.error("Client protocol error!");
					e.printStackTrace();
				} catch (IOException e) {
					logger.error("IO error!");
				}
			}
		}
	}
}

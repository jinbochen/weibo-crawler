package edu.bit.dlde.weibo_crawler.process;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.Seed;
import bit.mirror.data.WebPage;

import net.sf.json.JSONObject;

import edu.bit.dlde.weibo_crawler.core.Manager;
import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Producer;

/**
 * 用来存储数据的的processor，做的事情主要是把之前的东西包装成webpage，再入库
 */
public class WeiboSaver implements Processor<Long, JSONObject> {
	private static final Logger logger = LoggerFactory
			.getLogger(WeiboSaver.class);
	private MirrorEngineDao dao;// = new MongoDao();
	volatile private Producer<JSONObject> producer;
	private Manager manager;

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	private volatile static Long count = 0L;

	public Long produce() {
		return count;
	}

	/**
	 * none of sense
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Producer#produceMega()
	 */
	@Deprecated
	public Collection<Long> produceMega() {
		return null;
	}

	public void setProducer(Producer<JSONObject> p) {
		this.producer = p;
	}

	public Producer<JSONObject> getProducer() {
		return producer;
	}

	public void consume(Producer<JSONObject> p) throws Exception {
		this.producer = p;
		consume();
	}

	public void consume() throws Exception {
		if (producer == null) {
			logger.debug("Provider of weibo unavilable.");
			return;
		}
		JSONObject jsonObj = null;
		int sleep = 1;
		for (jsonObj = producer.produce(); true; jsonObj = producer.produce()) {
			if (jsonObj == null) {
				logger.debug("None of Weibo was found.");
				Thread.sleep(sleep * 1000);
				if (sleep < 60)
					sleep++;
				continue;
			}
			sleep = 1;
			Runnable r = new SaveThread(jsonObj);
			Manager.exec.execute(r);
		}
	}

	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.error("Error in saving weibo!");
		}
	}

	/**
	 * 用来存储weibo的线程
	 */
	public class SaveThread implements Runnable {
		JSONObject jsonObj = null;

		public SaveThread(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		public void run() {
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				/*** 将JSONObject转换成WebPage ***/
				WebPage webPage = new WebPage();
				String content = jsonObj.getString("content-" + i);
				if (content == null)
					break;
				webPage.setContent(content);
				webPage.setSeed((Seed) jsonObj.get("seed"));
				webPage.setFetchDate(new Date());
				try {
					webPage.setUrl(new URI(jsonObj.getString("uri")));
					/*** 将WebPage存入数据库 ***/
					dao.saveWebPage(webPage);
					count = count + 1;
				} catch (URISyntaxException e) {
					logger.error("Error in uri syntax.");
				}
			}
		}

	}
}

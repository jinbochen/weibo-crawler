package edu.bit.dlde.weibo_crawler.process;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
public class Saver implements Processor<Long, JSONObject> {
	private static final Logger logger = LoggerFactory.getLogger(Saver.class);
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

	ArrayList<Seed> cachedSeeds = new ArrayList<Seed>();

	private synchronized Seed getCachedSeed(String name) {
		for (Seed s : cachedSeeds) {
			if (s.getName().equals(name))
				return s;
		}
		Seed s = dao.getSeed(name);
		if (s != null)
			cachedSeeds.add(s);
		return s;
	}

	/**
	 * 一次处理一条微博，并未进行多线程
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Consumer#consume()
	 */
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
			SaveOperation saveOperation = new SaveOperation(jsonObj);
			saveOperation.run();
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
	 * 用来存储weibo的类，没有进行线程化
	 */
	public class SaveOperation {
		JSONObject jsonObj = null;

		public SaveOperation(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		public void run() throws InterruptedException {
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				/*** 将JSONObject转换成WebPage ***/
				if (!jsonObj.containsKey("content-" + i))
					break;
				// 似乎morphia本身就会有cache的机制，所以直接读是不是应该不会有太大的性能问题？不过没敢这么做
				Seed s = getCachedSeed(jsonObj.getString("seed:name"));
				if (s == null) {// 种子被更新,停下来
					manager.fireSeedReloadEvent();
					Thread.sleep(60 * 1000);
					return;
				}
				WebPage webPage = new WebPage();
				webPage.setSeed(s);
				String content = jsonObj.getString("content-" + i);
				webPage.setContent(content);
				webPage.setFetchDate(new Date());
				try {
					webPage.setUrl(new URI(jsonObj.getString("uri-" + i)));
					/*** 将WebPage存入数据库 ***/
					logger.info("{} was sunk into DB.", jsonObj.getString("uri-" + i));
					dao.saveWebPage(webPage);
					count = count + 1;
				} catch (URISyntaxException e) {
					logger.error("Error in uri syntax: {}.",
							jsonObj.getString("uri-" + i));
					e.printStackTrace();
				}
			}
		}

	}
}

package edu.bit.dlde.weibo_crawler.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.dao.MirrorEngineDao;

import net.sf.json.JSONObject;
import edu.bit.dlde.weibo_crawler.core.Manager;
import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Producer;

/**
 * 去重
 * 
 * @author lins
 * @date 2012-6-21
 **/
public class RedundancyFilter implements
		Processor<ArrayList<String>, JSONObject> {
	private static final Logger logger = LoggerFactory
			.getLogger(RedundancyFilter.class);
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
	private volatile static LinkedBlockingQueue<ArrayList<String>> weibos = new LinkedBlockingQueue<ArrayList<String>>();
	public ArrayList<String> produce() {
		return weibos.poll();
	}

	public Collection<ArrayList<String>> produceMega() {
		return weibos;
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
			logger.debug("Provider of weibo login unavilable.");
			return;
		}
		JSONObject jsonObj = null;
		int sleep = 1;
		for (jsonObj = producer.produce(); true; jsonObj = producer.produce()) {
			if(jsonObj==null){
				logger.debug("None of Weibo was found.");
				Thread.sleep(sleep * 1000);
				if (sleep < 60)
					sleep++;
				continue;
			}
			sleep = 1;
			// 处理每个jsonObj
			String data = jsonObj.getString("data");
		}
	}

	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.error("Error in filting weibo!");
		}
	}

}

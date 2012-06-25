package edu.bit.dlde.weibo_crawler.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
public class RedundancyFilter implements Processor<JSONObject, JSONObject> {
	private static final Logger logger = LoggerFactory
			.getLogger(RedundancyFilter.class);
	private MirrorEngineDao dao;// = new MongoDao();
	volatile private Producer<JSONObject> producer;
	private Manager manager;

	public RedundancyFilter() {
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Fail to create document builder!");
		}
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			logger.error("Fail to create transformer");
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("Fail to create transformer");
		}
	}

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

	private volatile static LinkedBlockingQueue<JSONObject> weibos = new LinkedBlockingQueue<JSONObject>();

	public JSONObject produce() {
		return weibos.poll();
	}

	public Collection<JSONObject> produceMega() {
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

	/** 处于性能考虑，两者都只有一份，通过synchronized来解决线程安全问题 **/
	private DocumentBuilder builder;
	private Transformer transformer;

	public void consume() throws Exception {
		if (producer == null) {
			logger.debug("Provider of weibo login unavilable.");
			return;
		}
		JSONObject jsonObj = null;
		int sleep = 1;
		for (jsonObj = producer.produce(); true; jsonObj = producer.produce()) {
			if (jsonObj == null) {
				logger.debug("None of Weibos were found.");
				Thread.sleep(sleep * 1000);
				if (sleep < 60)
					sleep++;
				continue;
			}
			sleep = 1;
			Runnable r = new FilterThread(jsonObj);
			Manager.exec.execute(r);
		}
	}

	public void run() {
		try {
			consume();
		} catch (Exception e) {
			logger.error("Error in filting weibo!");
		}
	}

	/**
	 * 用于跑过滤的线程,一个线程处理一个JSONObject
	 */
	public class FilterThread implements Runnable {
		JSONObject jsonObj = null;

		public FilterThread(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		private boolean DBcontains(String weibo) {
			return dao.containsWeibo(weibo);
		}

		int count = 0;
		boolean flag = false;
		public void run() {
			/***** 将string转换成dom ******/
			String data = jsonObj.getString("data");
			jsonObj.remove("data");
			data = "<root>" + data + "</root>";
			Document doc = null;
			synchronized (builder) {
				try {
					doc = builder.parse(new ByteArrayInputStream(data
							.getBytes()));
				} catch (SAXException e) {
					logger.error("Fail to parser data in JSON.");
				} catch (IOException e) {
					logger.error("Fail to parser data in JSON.");
				}
			}
			if (doc == null)
				return;
			/****** 再将dom所有的子节点转换成String *******/
			NodeList nodeList = doc.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node n = nodeList.item(i);
				DOMSource source = new DOMSource(n);
				StringWriter writer = new StringWriter();
				Result result = new StreamResult(writer);

				synchronized (transformer) {
					try {
						transformer.transform(source, result);
					} catch (TransformerException e) {
						logger.error("Fail to transform node to string.");
					}
				}

				/***** 通过对比数据库里面的String来判断冗余 ****/
				String content = writer.getBuffer().toString();
				if (!DBcontains(content)) {
					jsonObj.accumulate("content-" + i, content);
					count = 0;
					flag = true;
				} else {
					count++;
					if (i == nodeList.getLength() - 1
							&& count >= nodeList.getLength() / 2) {
						manager.fireFetcherReset();
						count = 0;
						break;
					}
				}
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Fail to close StringWriter.");
				}
			}
			/*** 将多个微博记录一并存进队列 ***/
			if (flag)
				weibos.add(jsonObj);
		}

	}
}

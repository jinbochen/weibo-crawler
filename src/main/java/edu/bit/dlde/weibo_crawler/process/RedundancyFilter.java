package edu.bit.dlde.weibo_crawler.process;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.data.WebPage;

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
	volatile private Manager manager;

	public RedundancyFilter() {
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
		} catch (TransformerConfigurationException e) {
			logger.error("Fail to create transformer.");
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("Fail to create transformer.");
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

	/** 出于性能考虑，只有一份，通过synchronized来解决线程安全问题 **/
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
			if (weibos.size() > MAX_VOLUMN) {
				logger.error("Sth. may be wrong with WeiboSaver as so many weibos in RedundancyFilter.");
				Thread.sleep(sleep * 1000);
				if (sleep <= 60)
					sleep++;
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

	public final int MAX_VOLUMN = 1024 * 15;

	/**
	 * 用于跑过滤的线程,一个线程处理一个JSONObject
	 */
	public class FilterThread implements Runnable {
		JSONObject jsonObj = null;

		public FilterThread(JSONObject jsonObj) {
			this.jsonObj = jsonObj;
		}

		long hash = 0;

		private boolean DBcontains(String raw, String content) {
			logger.debug("Caculating hash code of {}.", raw);
			// 模拟hashcode，不同的是返回long型
			// hash能保证的是相同的hash相同，但无法保证不同的不同
			long seed = 31;
			hash = 0;
			for (int i = 0; i < raw.length(); i++) {
				hash = (hash * seed) + raw.charAt(i);
			}
			//最多查询20次数据库，假如还是没找到就放弃该条微博，认为已经存在于数据库
			WebPage webPage;
			for (int i = 0; i < 20; i++) {
				webPage = dao.getWebPageByUrl(String.valueOf(hash));
				if (webPage == null)
					return false;
				else {
					if(webPage.getContent().equals(content))
						break;
					hash++;
				}
			}
			return true;
		}

		int count = 0;// 计数，记录到底有几个被视为重复
		boolean flag = false;

		public void run() {
			/***** 将string转换成dom ******/
			String data = jsonObj.getString("data");
			jsonObj.remove("data");
			// data = "<html>" + data + "</html>";
			HtmlCleaner cleaner = new HtmlCleaner();
			cleaner.getProperties().setNamespacesAware(false);
			Document doc = null;
			try {
				doc = new DomSerializer(cleaner.getProperties(), true)
						.createDOM(cleaner.clean(data));
			} catch (ParserConfigurationException e1) {
				logger.error("Error in cleaner configuration.");
			}
			// Document doc = null;
			// synchronized (builder) {
			// try {
			// doc = builder.parse(new ByteArrayInputStream(data
			// .getBytes()));
			// } catch (SAXException e) {
			// e.printStackTrace();
			// logger.error("Fail to parser data in JSON.");
			// return;
			// } catch (IOException e) {
			// logger.error("Fail to parser data in JSON.");
			// return;
			// }
			// }
			if (doc == null)
				return;
			/****** 再将dom所有的子节点转换成String *******/
			NodeList nodeList = doc.getElementsByTagName("body").item(0)
					.getChildNodes();
			for (int i = 0, j = 0; i < nodeList.getLength(); i++) {
				Node n = nodeList.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE)
					continue;
				DOMSource source = new DOMSource(n);
				StringWriter writer = new StringWriter();
				Result result = new StreamResult(writer);

				synchronized (transformer) {
					try {
						transformer.transform(source, result);
					} catch (TransformerException e) {
						logger.error("Fail to transform node to string.");
						return;
					}
				}

				/***** 通过对比数据库里面的String来判断冗余 ****/
				String content = writer.getBuffer().toString();
				String raw = n.getTextContent().trim();

				if (!DBcontains(raw, content)) {
					jsonObj.accumulate("content-" + j, content);
					/*** 由于uri是被索引的域所以计划使用hascode来代替，从而提升查重的速率 ***/
					jsonObj.accumulate("uri", String.valueOf(hash));
					j++;
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

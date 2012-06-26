package edu.bit.dlde.weibo_crawler.process;

import java.util.Collection;

import net.sf.json.JSONObject;
import edu.bit.dlde.weibo_crawler.core.Manager;
import edu.bit.dlde.weibo_crawler.core.Processor;
import edu.bit.dlde.weibo_crawler.core.Producer;

/**
 *获取评论放到json里面，然后从评论里面获得新的ajaxurl给予Fetcher
 *@author lins 2012-6-26
 */
public class CommentExtractor implements Processor<JSONObject, JSONObject>{

	public JSONObject produce() {
		return null;
	}

	public Collection<JSONObject> produceMega() {
		return null;
	}

	public void setProducer(Producer<JSONObject> p) {
		
	}

	public Producer<JSONObject> getProducer() {
		return null;
	}

	public void consume(Producer<JSONObject> p) throws Exception {
		
	}

	public void consume() throws Exception {
		
	}

	public void run() {
		
	}

	public Manager getManager() {
		return null;
	}

	public void setManager(Manager manager) {
		
	}

}

package edu.bit.dlde.weibo_crawler.core;

/**
 * 
 * @author lins
 * @date 2012-6-19
 **/
public interface Consumer<T> extends Runnable{
	public void setProducer(Producer<T> p);
	public Producer<T> getProducer();
	public void consume(Producer<T> p) throws Exception;
	public void consume() throws Exception;
}

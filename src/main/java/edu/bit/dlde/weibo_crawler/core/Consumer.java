package edu.bit.dlde.weibo_crawler.core;

/**
 * 
 * @author lins
 * @date 2012-6-19
 **/
public interface Consumer<T> extends Runnable{
	public void setProvider(Provider<T> p);
	public Provider<T> getProvider();
	public void consume(Provider<T> p) throws Exception;
	public void consume() throws Exception;
}

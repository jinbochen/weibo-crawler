package edu.bit.dlde.weibo_crawler.core;

/**
 * Processor<T, Z> which produces T and consumes Z.
 * @author lins
 * @date 2012-6-19
 **/
public interface Processor<T, Z> extends Producer<T>, Consumer<Z>, Runnable {
	public Manager getManager();
	public void setManager(Manager manager);
}

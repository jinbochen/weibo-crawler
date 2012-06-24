package edu.bit.dlde.weibo_crawler.core;

import java.util.Collection;

/**
 * 生产者
 * @author lins
 * @date 2012-6-19
 **/
public interface Producer<T>{
	public T produce();
	public Collection<T> produceMega();
}

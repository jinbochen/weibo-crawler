package edu.bit.dlde.weibo_crawler.core;

import java.util.Collection;

/**
 * 事实上这个不仅仅是个生产者还是监听这
 * @author lins
 * @date 2012-6-19
 **/
public interface Provider<T>{
	public T produce();
	public Collection<T> produceMega();
	public void notifyMyself();
}

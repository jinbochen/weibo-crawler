package edu.bit.dlde.weibo_crawler.core;

/**
 * 
 * @author lins
 * @date 2012-6-19
 **/
public interface Processor<T, Z> extends Provider<T>, Consumer<Z> {

}

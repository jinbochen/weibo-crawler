package edu.bit.dlde.weibo_crawler.facet;

/**
 *
 *@author lins
 *@date 2012-6-18
 **/
public interface WeiboCrawler{
	public boolean start();
	public boolean stop();
	public boolean pause(long millis);
	public boolean goon();
	public int getStatus();
}

package edu.bit.dlde.weibo_crawler.auth;
/**
 *
 *@author  吴少凯 <464289588@qq.com>
 *@modifiedBy lins
 *@date 2012-6-6
 **/
public interface LoginAuth {
	public String getAccount();
	public void setAccount(String account);
	public String getPassword();
	public void setPassword(String password);
	public String getCookie();
	
}

package edu.bit.dlde.weibo_crawler;

import java.io.IOException;

import org.apache.http.impl.client.DefaultHttpClient;

import edu.bit.dlde.weibo_crawler.auth.LoginFailureException;
import edu.bit.dlde.weibo_crawler.auth.SinaWeiboLoginAuth;

/**
 * 
 * @author lins 2012-6-26
 */
public class Hello {

	/**
	 * @param args
	 * @throws IOException
	 * @throws LoginFailureException
	 */
	public static void main(String[] args) throws LoginFailureException,
			IOException {
		System.out.println(Integer.MAX_VALUE);
		System.out.println(Long.MAX_VALUE);
		SinaWeiboLoginAuth sinaLogin = new SinaWeiboLoginAuth(
				new DefaultHttpClient());
		sinaLogin.try2Login("13811238365", "a62055974");
		System.out.println(sinaLogin.getCookie());
	}

}

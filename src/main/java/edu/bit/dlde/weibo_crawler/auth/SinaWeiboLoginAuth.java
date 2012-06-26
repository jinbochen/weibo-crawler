package edu.bit.dlde.weibo_crawler.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bit.dlde.weibo_crawler.utils.StreamFormator;

/**
 * 用于新浪微薄验证的类 主要是通过生成cookie来解决验证问题
 * 
 * @author 吴少凯 <464289588@qq.com>
 * @modifiedBy lins
 * @date 2012-6-6
 **/
public class SinaWeiboLoginAuth implements LoginAuth {
	private static final Logger logger = LoggerFactory
			.getLogger(SinaWeiboLoginAuth.class);

	private DefaultHttpClient dhc;
	private String nonce;
	private String su;
	private String servertime;
	private String sp;
	private String cookie;// httpclient链接是必须提交的参数表示已经登录了
	private String account;
	private String password;

	public HttpClient getHttpClient() {
		return dhc;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getCookie() {
		return cookie;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean try2Login(String account, String password)
			throws LoginFailureException, IOException {
		this.account = account;
		this.password = password;
		this.su = this.encodeAccount(this.account);
		this.nonce = this.makeNonce(6);
		this.servertime = this.getServerTime();
		this.sp = new SinaSSOEncoder().encode(this.password, this.servertime,
				this.nonce);
		this.cookie = this.initCookieString();
		return connect();
	}

	public SinaWeiboLoginAuth(DefaultHttpClient dhc) {
		this.dhc = dhc;
	}

	private final String loginUrl = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.3.16)";

	private boolean connect() throws LoginFailureException, IOException {
		boolean flag = false;
		
		if (this.dhc == null) {
			logger.error("DefaultHttpClient has not been initiallized!");
			throw new LoginFailureException();
		}

		logger.info("Login to retrieve cookies from server...");
		HttpPost post = new HttpPost(loginUrl);
		ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("entry", "weibo"));
		params.add(new BasicNameValuePair("gateway", "1"));
		params.add(new BasicNameValuePair("from", ""));
		params.add(new BasicNameValuePair("savestate", "7"));
		params.add(new BasicNameValuePair("useticket", "1"));
		params.add(new BasicNameValuePair("ssosimplelogin", "1"));
		params.add(new BasicNameValuePair("vsnf", "1"));
		params.add(new BasicNameValuePair("vsnval", ""));
		params.add(new BasicNameValuePair("su", this.su));
		params.add(new BasicNameValuePair("service", "miniblog"));
		params.add(new BasicNameValuePair("servertime", this.servertime));
		params.add(new BasicNameValuePair("nonce", this.nonce));
		params.add(new BasicNameValuePair("pwencode", "wsse"));
		params.add(new BasicNameValuePair("sp", this.sp));
		params.add(new BasicNameValuePair("encoding", "UTF-8"));
		// params.add(new BasicNameValuePair(
		// "url",
		// "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
		params.add(new BasicNameValuePair("returntype", "META"));
		UrlEncodedFormEntity formEntiry = new UrlEncodedFormEntity(params);
		post.setEntity(formEntiry);
		HttpResponse hr = this.dhc.execute(post);
		// 添加cookie参数
		String cookiestr;
		for (int i = 0; i < hr.getHeaders("Set-Cookie").length; i++) {
			cookiestr = hr.getHeaders("Set-Cookie")[i].toString()
					.replace("Set-Cookie:", "").trim();
			this.cookie += cookiestr.substring(0, cookiestr.indexOf(";")) + ";";
		}
		this.cookie += "un=" + this.account;

		HttpEntity hentity = hr.getEntity();
		InputStream inputstream = hentity.getContent();
		String tmp = StreamFormator.getString(inputstream, "gbk");
		if (tmp.indexOf("正在登录") > 0) {
			flag = true;
			logger.info("Success to login!");
		} else {
			flag = false;
			throw new LoginFailureException();
		}
		inputstream.close();
		
		return flag;
	}

	private String initCookieString() {
		StringBuilder sb = new StringBuilder();
		sb.append("wvr=4;");
		return sb.toString();
	}

	@SuppressWarnings("deprecation")
	private String encodeAccount(String account) {
		return Base64.encodeBase64String(URLEncoder.encode(account).getBytes());
	}

	private String makeNonce(int len) {
		String x = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String str = "";
		for (int i = 0; i < len; i++) {
			str += x.charAt((int) (Math.ceil(Math.random() * 1000000) % x
					.length()));
		}
		return str;
	}

	private String getServerTime() {
		long servertime = new Date().getTime() / 1000;
		return String.valueOf(servertime);
	}

	public static void main(String[] args) throws LoginFailureException,
			IOException {
		SinaWeiboLoginAuth sinaLogin = new SinaWeiboLoginAuth(
				new DefaultHttpClient());
		sinaLogin.try2Login("13811238365", "a62055974");
		System.out.println(sinaLogin.getCookie());
	}
}

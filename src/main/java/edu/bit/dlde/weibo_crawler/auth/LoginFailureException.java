package edu.bit.dlde.weibo_crawler.auth;

/**
 * 登录错误异常
 * @author lins
 * @date 2012-6-19
 **/
public class LoginFailureException extends Exception {
	private static final long serialVersionUID = 1578506640715055049L;

	public LoginFailureException() {
	}

	public LoginFailureException(String msg) {
		super(msg);
	}
}

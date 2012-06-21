package edu.bit.dlde.weibo_crawler.process;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bit.mirror.dao.MirrorEngineDao;
import bit.mirror.dao.mongo.MongoDao;
import bit.mirror.data.Seed;

import edu.bit.dlde.weibo_crawler.core.Provider;

/**
 * 为SinaWeiboLogin提供seed
 * 
 * @author lins
 * @date 2012-6-19
 **/
public class SeedProvider implements Provider<Seed> {
	private Logger logger = LoggerFactory.getLogger(SeedProvider.class);
	private MirrorEngineDao dao = new MongoDao();

	public SeedProvider(MirrorEngineDao dao) {
		this.dao = dao;
		notifyMyself();
	}

	public MirrorEngineDao getDao() {
		return dao;
	}

	public void setDao(MirrorEngineDao dao) {
		this.dao = dao;
	}

	SynchronousQueue<Seed> seeds;

	/**
	 * 发派种子，每次从队列头部弹出一个种子。由于provider由consumer持有，所以种子的更新交由consumer处理
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#produce()
	 */
	public Seed produce() {
		if (seeds == null || seeds.size() == 0)
			return null;
		try {
			return seeds.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Interrupted while getting seeds...");
			return null;
		}
	}

	/**
	 * 发派一堆种子，但是不会清空队列
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#produceMega()
	 */
	public Collection<Seed> produceMega() {
		return seeds;
	}

	/**
	 * 从mongo数据库读入所有seed
	 * 
	 * @see edu.bit.dlde.weibo_crawler.core.Provider#notifyMyself()
	 */
	public synchronized void notifyMyself() {
		seeds = new SynchronousQueue<Seed>();
		Iterator<Seed> it = dao.getSeeds().iterator();
		while (it.hasNext()) {
			Seed s = it.next();
			if (s.getType().equals("WEIBO"))
				seeds.add(it.next());
		}
	}

}

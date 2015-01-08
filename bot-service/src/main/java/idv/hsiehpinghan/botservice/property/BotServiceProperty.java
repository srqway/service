package idv.hsiehpinghan.botservice.property;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BotServiceProperty implements InitializingBean {
	private String downloadDir;

	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
		String pDownloadDir = "bot-service.download_dir";
		downloadDir = environment.getProperty(pDownloadDir);
		if (downloadDir == null) {
			throw new RuntimeException(pDownloadDir + " not set !!!");
		}
	}

	public String getDownloadDir() {
		return downloadDir;
	}

}

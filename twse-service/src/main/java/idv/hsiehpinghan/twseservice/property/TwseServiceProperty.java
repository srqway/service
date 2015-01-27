package idv.hsiehpinghan.twseservice.property;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TwseServiceProperty implements InitializingBean {
	private String downloadDir;

	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
		processDownloadDir();
	}

	public String getDownloadDir() {
		return downloadDir;
	}

	private void processDownloadDir() {
		String pDownloadDir = "twse-service.download_dir";
		downloadDir = environment.getProperty(pDownloadDir);
		if (downloadDir == null) {
			throw new RuntimeException(pDownloadDir + " not set !!!");
		}
	}

}

package idv.hsiehpinghan.stockservice.property;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StockServiceProperty implements InitializingBean {
	private String downloadDir;
	private String extractDir;

	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
		processDownloadDir();
		processExtractDir();
	}

	public String getDownloadDir() {
		return downloadDir;
	}

	public String getExtractDir() {
		return extractDir;
	}

	private void processDownloadDir() {
		String pDownloadDir = "stock-service.download_dir";
		downloadDir = environment.getProperty(pDownloadDir);
		if (downloadDir == null) {
			throw new RuntimeException(pDownloadDir + " not set !!!");
		}
	}

	private void processExtractDir() {
		String pExtractDir = "stock-service.extract_dir";
		extractDir = environment.getProperty(pExtractDir);
		if (extractDir == null) {
			throw new RuntimeException(pExtractDir + " not set !!!");
		}
	}
}
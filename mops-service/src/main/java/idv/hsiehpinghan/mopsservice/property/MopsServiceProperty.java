package idv.hsiehpinghan.mopsservice.property;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MopsServiceProperty implements InitializingBean {
//	private String hdfsXbrlDir;
	private String downloadDir;
	private String extractDir;

	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
//		processHdfsXbrlDir();
		processDownloadDir();
		processExtractDir();
	}

	public String getDownloadDir() {
		return downloadDir;
	}

//	public String getHdfsXbrlDir() {
//		return hdfsXbrlDir;
//	}

	public String getExtractDir() {
		return extractDir;
	}

//	private void processHdfsXbrlDir() {
//		String pHdfsXbrlDir = "mops-service.hdfs_xbrl_dir";
//		hdfsXbrlDir = environment.getProperty(pHdfsXbrlDir);
//		if (hdfsXbrlDir == null) {
//			throw new RuntimeException(pHdfsXbrlDir + " not set !!!");
//		}
//	}

	private void processDownloadDir() {
		String pDownloadDir = "mops-service.download_dir";
		downloadDir = environment.getProperty(pDownloadDir);
		if (downloadDir == null) {
			throw new RuntimeException(pDownloadDir + " not set !!!");
		}
	}

	private void processExtractDir() {
		String pExtractDir = "mops-service.extract_dir";
		String extractDir = environment.getProperty(pExtractDir);
		if (extractDir == null) {
			throw new RuntimeException(pExtractDir + " not set !!!");
		}
	}
}

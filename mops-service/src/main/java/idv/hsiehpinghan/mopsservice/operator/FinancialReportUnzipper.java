package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.compressutility.utility.CompressUtility;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportUnzipper implements InitializingBean {
	private final int MAX_TRY_AMOUNT = 3;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File extractDir;

	@Autowired
	private Environment environment;

	/**
	 * Repeat try unzip.
	 * @param file
	 * @return
	 */
	public File repeatTryUnzip(File file) {
		int tryAmount = 0;
		while (true) {
			File dir = null;
			try {
				dir = CompressUtility.unzip(file, extractDir);
				logger.info("Unzipp to " + dir + " success.");
				return dir;
			} catch (Exception e) {
				++tryAmount;
				logger.warn("Unzip fail " + tryAmount + " times !!!");
				if (tryAmount >= MAX_TRY_AMOUNT) {
					logger.warn("File("
							+ file.getAbsolutePath()
							+ ") delete "
							+ (file.delete() == true ? " success !!!"
									: " failed !!!"));
					logger.warn("Directory("
							+ dir.getAbsolutePath()
							+ ") delete "
							+ (dir.delete() == true ? " success !!!"
									: " failed !!!"));
					throw new RuntimeException(e);
				}
				sleep(tryAmount * 60);
			}
		}
	}

	public File getExtractDir() {
		return extractDir;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		String eStr = "mops-service.extract_dir";
		String eProp = environment.getProperty(eStr);
		if (eProp == null) {
			throw new RuntimeException(eStr + " not set !!!");
		}
		extractDir = new File(eProp);
	}

	private void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			logger.warn("Exception : ", e);
		}
	}
}

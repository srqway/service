package idv.hsiehpinghan.mopsservice.operator.utility;

import idv.hsiehpinghan.mopsservice.webelement.XbrlDownloadTable;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.openqa.selenium.support.ui.FluentWait;

import com.google.common.base.Function;

public class MopsAjaxWaitUtility {
	private static final int POLLING_MILLISECONDS = 1000;
	private static final int TIMEOUT_MILLISECONDS = 10000;
	private static Logger logger = Logger.getLogger(MopsAjaxWaitUtility.class
			.getName());

	/**
	 * Wait any button's onclick attribute like text.
	 * 
	 * @param table
	 * @param text
	 * @return
	 */
	public static boolean waitUntilAnyButtonOnclickAttributeLike(
			final XbrlDownloadTable table, final String regex) {
		// Object parameter is not used.
		FluentWait<Object> fluentWait = new FluentWait<Object>(new Object());
		fluentWait.pollingEvery(POLLING_MILLISECONDS, TimeUnit.MILLISECONDS);
		fluentWait.withTimeout(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
		return fluentWait.until(new Function<Object, Boolean>() {
			@Override
			public Boolean apply(Object obj) {
				try {
					// i = 0 is title.
					for (int i = 1, size = table.getRowSize(); i < size; ++i) {
						String fileName = table.getDownloadFileName(i);
						if(fileName != null && fileName.matches(regex)) {
							return true;
						}
					}
					return false;
				} catch (Exception e) {
					logger.trace("Exception : ", e);
					return false;
				}
			}
		});
	}

}

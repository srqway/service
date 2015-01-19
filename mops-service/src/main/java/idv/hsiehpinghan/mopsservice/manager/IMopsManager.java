package idv.hsiehpinghan.mopsservice.manager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface IMopsManager {
	boolean updateFinancialReportPresentation();

	boolean updateFinancialReportInstance() throws IllegalAccessException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException;
}

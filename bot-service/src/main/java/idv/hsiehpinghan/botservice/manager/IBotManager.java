package idv.hsiehpinghan.botservice.manager;

import java.io.File;

public interface IBotManager {
	File downloadExchangeRate();
	boolean saveExchangeRateToDatabase(File dataDirectory);
}

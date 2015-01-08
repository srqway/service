package idv.hsiehpinghan.botservice.manager.hbase;

import idv.hsiehpinghan.botservice.enumeration.Dollar;
import idv.hsiehpinghan.botservice.manager.IBotManager;
import idv.hsiehpinghan.botservice.operator.ExchangeRateDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BotHbaseManager implements IBotManager {
	// private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<Dollar> targetDallars;
	
	@Autowired
	private ExchangeRateDownloader downloader;

	public BotHbaseManager() {
		targetDallars = new ArrayList<Dollar>(1);
		targetDallars.add(Dollar.USD);
	}

	public File downloadExchangeRate() {
		return downloader.downloadExchangeRate(targetDallars);
	}

	public boolean saveExchangeRateToDatabase(File dataDirectory) {
		// TODO Auto-generated method stub
		return false;
	}
}

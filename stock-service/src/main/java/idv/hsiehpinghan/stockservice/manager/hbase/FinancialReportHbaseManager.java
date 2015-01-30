package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.stockdao.entity.StockDownloadInfo;
import idv.hsiehpinghan.stockdao.enumeration.Dollar;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.IFinancialReportInstanceRepository;
import idv.hsiehpinghan.stockdao.repository.IFinancialReportPresentationRepository;
import idv.hsiehpinghan.stockdao.repository.IStockDownloadInfoRepository;
import idv.hsiehpinghan.stockservice.manager.IFinancialReportManager;
import idv.hsiehpinghan.stockservice.operator.ExchangeRateDownloader;
import idv.hsiehpinghan.stockservice.operator.FinancialReportCalculator;
import idv.hsiehpinghan.stockservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.stockservice.operator.FinancialReportJsonMaker;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.assistant.TaxonomyAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportHbaseManager implements IFinancialReportManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<String> presentIds;
	private List<Dollar> targetDallars;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private ExchangeRateDownloader exchangeRateDownloader;
	@Autowired
	private FinancialReportCalculator calculator;
	@Autowired
	private FinancialReportJsonMaker jsonMaker;
	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private TaxonomyAssistant taxonomyAssistant;
	@Autowired
	private InstanceAssistant instanceAssistant;
	@Autowired
	private IFinancialReportPresentationRepository presentRepo;
	@Autowired
	private IFinancialReportInstanceRepository instanceRepo;
	@Autowired
	private IStockDownloadInfoRepository infoRepo;

	public FinancialReportHbaseManager() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);

		targetDallars = new ArrayList<Dollar>(1);
		targetDallars.add(Dollar.USD);
	}

	@Override
	public boolean updateFinancialReportPresentation() {
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		XbrlTaxonomyVersion version = null;
		try {
			for (int i = 0, size = versions.length; i < size; ++i) {
				version = versions[i];
				ObjectNode presentNode = taxonomyAssistant.getPresentationJson(
						version, presentIds);
				if (presentRepo.exists(version)) {
					logger.info(version + " exists.");
					continue;
				}
				presentRepo.put(version, presentIds, presentNode);
				logger.info(version + " updated.");
			}
			logger.info("Update financial report presentation finished.");
		} catch (Exception e) {
			logger.error(version + " update fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateFinancialReportInstance() {
		File xbrlDir = downloadFinancialReportInstance();
		if (xbrlDir == null) {
			return false;
		}
		try {
			StockDownloadInfo downloadInfo = getDownloadInfoEntity();
			int processFilesAmt = saveFinancialReportToHBase(xbrlDir,
					downloadInfo);
			infoRepo.put(downloadInfo);
			logger.info("Saved " + processFilesAmt + " xbrl files to "
					+ instanceRepo.getTargetTableName() + ".");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateExchangeRate() {
		File exchangeDir = downloadExchangeRate();
		saveExchangeRateToDatabase(exchangeDir);
		return true;
	}

	@Override
	public boolean calculateFinancialReport() {
		try {
			StockDownloadInfo downloadInfo = infoRepo.get(instanceRepo
					.getTargetTableName());
			calculator.calculate(downloadInfo);
		} catch (Exception e) {
			logger.error("Calculate financial report fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public StockDownloadInfo getFinancialReportDownloadInfo() {
		try {
			return infoRepo.get(instanceRepo.getTargetTableName());
		} catch (Exception e) {
			logger.error("Get download info fail !!!");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Map<String, ObjectNode> getFinancialReportDetailJsonMap(
			String stockCode, ReportType reportType, Integer year,
			Integer season) {
		try {
			return jsonMaker.getPresentationJsonMap(presentIds, stockCode,
					reportType, year, season);
		} catch (Exception e) {
			logger.error("Get presentation json map fail !!!");
			e.printStackTrace();
			return null;
		}
	}

	StockDownloadInfo getDownloadInfoEntity() throws IllegalAccessException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		String tableName = instanceRepo.getTargetTableName();
		StockDownloadInfo entity = infoRepo.get(tableName);
		if (entity == null) {
			entity = new StockDownloadInfo();
			entity.new RowKey(tableName, entity);
		}
		return entity;
	}

	File downloadFinancialReportInstance() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download financial report fail !!!");
			return null;
		}
	}

	int saveFinancialReportToHBase(File xbrlDir, StockDownloadInfo downloadInfo)
			throws Exception {
		return processXbrlFiles(xbrlDir, downloadInfo);
	}

	int processXbrlFiles(File file, StockDownloadInfo downloadInfo)
			throws Exception {
		int count = 0;
		if (file.isDirectory()) {
			File[] fs = file.listFiles();
			for (File f : fs) {
				count += processXbrlFiles(f, downloadInfo);
			}
		} else {
			// ex. tifrs-fr0-m1-ci-cr-1101-2013Q1.xml
			String[] strArr = file.getName().split("-");
			String stockCode = strArr[5];
			ReportType reportType = ReportType.getReportType(strArr[4]);
			int year = Integer.valueOf(strArr[6].substring(0, 4));
			int season = Integer.valueOf(strArr[6].substring(5, 6));
			ObjectNode objNode = instanceAssistant.getInstanceJson(file,
					presentIds);
			if (instanceRepo.exists(stockCode, reportType, year, season) == false) {
				instanceRepo.put(stockCode, reportType, year, season, objNode,
						presentIds);
				logger.info(file.getName() + " saved to "
						+ instanceRepo.getTargetTableName() + ".");
			} else {
				logger.info(file.getName() + " already saved to "
						+ instanceRepo.getTargetTableName() + ".");
			}
			addToDownloadInfoEntity(downloadInfo, stockCode, reportType, year,
					season);
			++count;
		}
		return count;
	}

	private void addToDownloadInfoEntity(StockDownloadInfo downloadInfo,
			String stockCode, ReportType reportType, int year, int season)
			throws IllegalAccessException {
		Date date = Calendar.getInstance().getTime();
		addStockCode(downloadInfo, date, stockCode);
		addReportType(downloadInfo, date, reportType);
		addYear(downloadInfo, date, year);
		addSeason(downloadInfo, date, season);
	}

	private void addStockCode(StockDownloadInfo downloadInfo, Date date,
			String stockCode) {
		String all = StockDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
		downloadInfo.getStockCodeFamily().addStockCode(all, date, stockCode);
	}

	private void addReportType(StockDownloadInfo downloadInfo, Date date,
			ReportType reportType) {
		String all = StockDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
		downloadInfo.getReportTypeFamily().addReportType(all, date, reportType);
	}

	private void addYear(StockDownloadInfo downloadInfo, Date date, int year) {
		String all = StockDownloadInfo.YearFamily.YearQualifier.ALL;
		downloadInfo.getYearFamily().addYear(all, date, year);
	}

	private void addSeason(StockDownloadInfo downloadInfo, Date date, int season) {
		String all = StockDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
		downloadInfo.getSeasonFamily().addSeason(all, date, season);
	}

	private File downloadExchangeRate() {
		return exchangeRateDownloader.downloadExchangeRate(targetDallars);
	}

	private boolean saveExchangeRateToDatabase(File dataDirectory) {
		// TODO Auto-generated method stub
		return false;
	}
}
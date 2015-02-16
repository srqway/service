package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.StockInfo.RowKey;
import idv.hsiehpinghan.stockdao.entity.Taxonomy;
import idv.hsiehpinghan.stockdao.entity.Taxonomy.PresentationFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.StockInfoRepository;
import idv.hsiehpinghan.stockdao.repository.TaxonomyRepository;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.stockservice.manager.IFinancialReportManager;
import idv.hsiehpinghan.stockservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.stockservice.operator.FinancialReportJsonMaker;
import idv.hsiehpinghan.stockservice.operator.XbrlInstanceConverter;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.assistant.TaxonomyAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportHbaseManager implements IFinancialReportManager,
		InitializingBean {
	private final String[] EXTENSIONS = { "xml" };
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<String> presentIds;
	// private List<Dollar> targetDallars;
	private File extractDir;
	private File processedLog;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private XbrlInstanceConverter converter;

	// @Autowired
	// private ExchangeRateDownloader exchangeRateDownloader;
	// @Autowired
	// private FinancialReportCalculator calculator;
	@Autowired
	private FinancialReportJsonMaker jsonMaker;
	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private TaxonomyAssistant taxonomyAssistant;
	@Autowired
	private InstanceAssistant instanceAssistant;
	@Autowired
	private TaxonomyRepository taxonomyRepo;
	@Autowired
	private XbrlRepository xbrlRepo;
	@Autowired
	private StockInfoRepository infoRepo;

	public FinancialReportHbaseManager() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);

		// targetDallars = new ArrayList<Dollar>(1);
		// targetDallars.add(Dollar.USD);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		extractDir = stockServiceProperty.getFinancialReportExtractDir();
		generateProcessedLog();
	}

	@Override
	public boolean updateTaxonomyPresentation() {
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		XbrlTaxonomyVersion version = null;
		Date ver = Calendar.getInstance().getTime();
		try {
			for (int i = 0, size = versions.length; i < size; ++i) {
				version = versions[i];
				ObjectNode presentNode = taxonomyAssistant.getPresentationJson(
						version, presentIds);
				if (taxonomyRepo.exists(version)) {
					logger.info(version + " exists.");
					continue;
				}
				Taxonomy entity = taxonomyRepo.generateEntity(version);
				generatePresentationFamilyContent(entity, ver, presentNode);
				taxonomyRepo.put(entity);
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
	public boolean updateXbrlInstance() {
		File xbrlDir = downloadFinancialReportInstance();
		if (xbrlDir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveFinancialReportToHBase(xbrlDir);
			logger.info("Saved " + processFilesAmt + " xbrl files to "
					+ xbrlRepo.getTargetTableName() + ".");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public List<String> getStockCodes() {
		List<RowKey> rowKeys = infoRepo.getRowKeys();
		List<String> stockCodes = new ArrayList<String>(rowKeys.size());
		for (RowKey rowKey : rowKeys) {
			stockCodes.add(rowKey.getStockCode());
		}
		return stockCodes;
	}

	// @Override
	// public boolean updateExchangeRate() {
	// File exchangeDir = downloadExchangeRate();
	// saveExchangeRateToDatabase(exchangeDir);
	// return true;
	// }

	// @Override
	// public boolean calculateFinancialReport() {
	// try {
	// StockDownloadInfo downloadInfo = infoRepo.get(instanceRepo
	// .getTargetTableName());
	// calculator.calculate(downloadInfo);
	// } catch (Exception e) {
	// logger.error("Calculate financial report fail !!!");
	// e.printStackTrace();
	// return false;
	// }
	// return true;
	// }

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

	int saveFinancialReportToHBase(File xbrlDir) throws Exception {
		Set<String> processedSet = FileUtility.readLinesAsHashSet(processedLog);
		int count = 0;
		// ex. tifrs-fr0-m1-ci-cr-1101-2013Q1.xml
		for (File file : FileUtils.listFiles(xbrlDir, EXTENSIONS, true)) {
			processXbrlFiles(file, processedSet);
			++count;
		}
		return count;
	}

	void processXbrlFiles(File file, Set<String> processedSet) throws Exception {
		String[] strArr = file.getName().split("-");
		String stockCode = strArr[5];
		ReportType reportType = ReportType.getMopsReportType(strArr[4]);
		int year = Integer.valueOf(strArr[6].substring(0, 4));
		int season = Integer.valueOf(strArr[6].substring(5, 6));
		ObjectNode objNode = instanceAssistant
				.getInstanceJson(file, presentIds);
		if (isProcessed(processedSet, file) == false) {
			Xbrl entity = converter.convert(stockCode, reportType, year,
					season, objNode);
			xbrlRepo.put(entity);
			logger.info(file.getName() + " saved to "
					+ xbrlRepo.getTargetTableName() + ".");
			writeToProcessedFile(file);
		}
	}

	private void writeToProcessedFile(File file) throws IOException {
		String infoLine = generateProcessedInfo(file) + System.lineSeparator();
		FileUtils.write(processedLog, infoLine, Charsets.UTF_8, true);
	}

	private void generateProcessedLog() throws IOException {
		if (processedLog == null) {
			processedLog = new File(extractDir, "processed.log");
			if (processedLog.exists() == false) {
				FileUtils.touch(processedLog);
			}
		}
	}

	private boolean isProcessed(Set<String> processedSet, File file)
			throws IOException {
		String processedInfo = generateProcessedInfo(file);
		if (processedSet.contains(processedInfo)) {
			logger.info(processedInfo + " processed before.");
			return true;
		}
		return false;
	}

	private String generateProcessedInfo(File file) {
		return file.getName();
	}

	private void generatePresentationFamilyContent(Taxonomy entity, Date ver,
			ObjectNode presentNode) {
		PresentationFamily fam = entity.getPresentationFamily();
		fam.setBalanceSheet(ver, presentNode.get(Presentation.Id.BalanceSheet)
				.toString());
		fam.setStatementOfCashFlows(ver,
				presentNode.get(Presentation.Id.StatementOfCashFlows)
						.toString());
		fam.setStatementOfChangesInEquity(ver,
				presentNode.get(Presentation.Id.StatementOfChangesInEquity)
						.toString());
		fam.setStatementOfComprehensiveIncome(ver,
				presentNode.get(Presentation.Id.StatementOfComprehensiveIncome)
						.toString());
	}
}

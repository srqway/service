package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsdao.repository.MopsDownloadInfoRepository;
import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportCalculator;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;
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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class MopsHbaseManager implements IMopsManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<String> presentIds;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private FinancialReportCalculator calculator;
	@Autowired
	private HdfsAssistant hdfsAssistant;
	@Autowired
	private MopsServiceProperty mopsServiceProperty;
	@Autowired
	private TaxonomyAssistant taxonomyAssistant;
	@Autowired
	private InstanceAssistant instanceAssistant;
	@Autowired
	private FinancialReportPresentationRepository presentRepo;
	@Autowired
	private FinancialReportInstanceRepository instanceRepo;
	@Autowired
	private MopsDownloadInfoRepository infoRepo;

	public MopsHbaseManager() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);
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
			MopsDownloadInfo downloadInfo = getDownloadInfoEntity();
			int processFilesAmt = saveFinancialReportToHBase(xbrlDir,
					downloadInfo);
			infoRepo.put(downloadInfo);
			logger.info("Saved " + processFilesAmt + " xbrl files to hbase.");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean calculateFinancialReport() {
		try {
			MopsDownloadInfo downloadInfo = infoRepo.get(instanceRepo
					.getTargetTableName());
			calculator.calculate(downloadInfo);
		} catch (Exception e) {
			logger.error("Calculate financial report fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	MopsDownloadInfo getDownloadInfoEntity() throws IllegalAccessException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		String tableName = instanceRepo.getTargetTableName();
		MopsDownloadInfo entity = infoRepo.get(tableName);
		if (entity == null) {
			entity = new MopsDownloadInfo();
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

	int saveFinancialReportToHBase(File xbrlDir, MopsDownloadInfo downloadInfo)
			throws Exception {
		return processXbrlFiles(xbrlDir, downloadInfo);
	}

	int processXbrlFiles(File file, MopsDownloadInfo downloadInfo)
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

	private void addToDownloadInfoEntity(MopsDownloadInfo downloadInfo,
			String stockCode, ReportType reportType, int year, int season)
			throws IllegalAccessException {
		Date date = Calendar.getInstance().getTime();
		addStockCode(downloadInfo, date, stockCode);
		addReportType(downloadInfo, date, reportType);
		addYear(downloadInfo, date, year);
		addSeason(downloadInfo, date, season);
	}

	private void addStockCode(MopsDownloadInfo downloadInfo, Date date,
			String stockCode) {
		String all = MopsDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
		downloadInfo.getStockCodeFamily().addStockCode(all, date, stockCode);
	}

	private void addReportType(MopsDownloadInfo downloadInfo, Date date,
			ReportType reportType) {
		String all = MopsDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
		downloadInfo.getReportTypeFamily().addReportType(all, date, reportType);
	}

	private void addYear(MopsDownloadInfo downloadInfo, Date date, int year) {
		String all = MopsDownloadInfo.YearFamily.YearQualifier.ALL;
		downloadInfo.getYearFamily().addYear(all, date, year);
	}

	private void addSeason(MopsDownloadInfo downloadInfo, Date date, int season) {
		String all = MopsDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
		downloadInfo.getSeasonFamily().addSeason(all, date, season);
	}
}

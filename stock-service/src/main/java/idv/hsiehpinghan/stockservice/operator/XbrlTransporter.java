package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseColumnQualifier;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseValue;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily.RatioDifferenceQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily.RatioDifferenceValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RowKey;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class XbrlTransporter {
	// private Logger logger = Logger.getLogger(this.getClass().getName());
	private final String YYYY_MM_DD = "yyyy-MM-dd";
	private final Charset UTF_8 = Charsets.UTF_8;
	private final String NA = StringUtility.NA_STRING;
	private final String XBRL = "xbrl";

	@Autowired
	private XbrlRepository xbrlRepo;

	public void saveHbaseDataToFile(String stockCode, ReportType reportType,
			File targetDirectory) throws IOException {
		TreeSet<Xbrl> entities = xbrlRepo.fuzzyScan(stockCode, reportType,
				null, null);
		File targetFile = FileUtility.getOrCreateFile(targetDirectory, XBRL);
		FileUtils.write(targetFile, generateTitle(), UTF_8, false);
		for (Xbrl entity : entities) {
			writeToFile(targetFile, entity);
		}
	}

	private void writeToFile(File targetFile, Xbrl entity) throws IOException {
		RowKey rowKey = (RowKey) entity.getRowKey();
		String stockCode = rowKey.getStockCode();
		ReportType reportType = rowKey.getReportType();
		int year = rowKey.getYear();
		int season = rowKey.getSeason();
		RatioDifferenceFamily diffFam = entity.getRatioDifferenceFamily();
		for (Entry<HBaseColumnQualifier, HBaseValue> ent : diffFam
				.getLatestQualifierAndValueAsSet()) {
			RatioDifferenceQualifier qual = (RatioDifferenceQualifier) ent
					.getKey();
			String elementId = qual.getElementId();
			PeriodType periodType = qual.getPeriodType();
			Date instant = qual.getInstant();
			Date startDate = qual.getStartDate();
			Date endDate = qual.getEndDate();
			RatioDifferenceValue val = (RatioDifferenceValue) ent.getValue();
			BigDecimal ratioDifference = val.getAsBigDecimal();
			String record = generateRecord(stockCode, reportType, year, season,
					elementId, periodType, instant, startDate, endDate,
					ratioDifference);
			FileUtils.write(targetFile, record, UTF_8, true);
		}
	}

	private String generateTitle() {
		return "stockCode,reportType,year,season,elementId,periodType,instant,startDate,endDate,ratioDifference"
				+ System.lineSeparator();
	}

	private String generateRecord(String stockCode, ReportType reportType,
			int year, int season, String elementId, PeriodType periodType,
			Date instant, Date startDate, Date endDate,
			BigDecimal ratioDifference) {
		String instantStr = DateUtility.getDateString(instant, YYYY_MM_DD, NA);
		String startDateStr = DateUtility.getDateString(startDate, YYYY_MM_DD,
				NA);
		String endDateStr = DateUtility.getDateString(endDate, YYYY_MM_DD, NA);
		return stockCode + "," + reportType + "," + year + "," + season + ","
				+ elementId + "," + periodType + "," + instantStr + ","
				+ startDateStr + "," + endDateStr + "," + ratioDifference
				+ System.lineSeparator();
	}

}

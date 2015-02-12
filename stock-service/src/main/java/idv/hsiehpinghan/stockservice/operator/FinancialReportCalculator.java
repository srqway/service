package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseColumnQualifier;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily.InstanceQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.ItemFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RowKey;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportCalculator {
	private final String TWD = "TWD";
	private final String SHARES = "Shares";
	private final String DURATION = "duration";
	private final String INSTANT = "instant";

	private Logger logger = Logger.getLogger(this.getClass().getName());
//	@Autowired
//	private IFinancialReportDataRepository dataRepo;
//	@Autowired
//	private IFinancialReportInstanceRepository instanceRepo;

	public void calculate(StockDownloadInfo stockDownloadInfo)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, IOException {
		String allStockCode = StockDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
		String allReportType = StockDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
		String allYear = StockDownloadInfo.YearFamily.YearQualifier.ALL;
		String allSeason = StockDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
		StockCodeValue stockCodeValue = stockDownloadInfo.getStockCodeFamily()
				.getLatestValue(allStockCode);
		ReportTypeValue reportTypeValue = stockDownloadInfo
				.getReportTypeFamily().getLatestValue(allReportType);
		YearValue yearValue = stockDownloadInfo.getYearFamily().getLatestValue(
				allYear);
		SeasonValue seasonValue = stockDownloadInfo.getSeasonFamily()
				.getLatestValue(allSeason);

		int precessAmt = 0;
		for (String stockCode : stockCodeValue.getStockCodes()) {
			for (ReportType reportType : reportTypeValue.getReportTypes()) {
				for (Integer year : yearValue.getYears()) {
					for (Integer season : seasonValue.getSeasons()) {
						if (process(stockCode, reportType, year, season) == false) {
							continue;
						}
						FinancialReportData entity = generateEntity(stockCode,
								reportType, year, season);
						dataRepo.put(entity);
						++precessAmt;
						logSaveMsg(stockCode, reportType, year, season, entity);
					}
				}
			}
		}
		logger.info("Saved " + precessAmt + " xbrl files to "
				+ dataRepo.getTargetTableName() + ".");
	}

//	private boolean process(String stockCode, ReportType reportType,
//			Integer year, Integer season) throws NoSuchFieldException,
//			SecurityException, IllegalArgumentException,
//			IllegalAccessException, NoSuchMethodException,
//			InvocationTargetException, InstantiationException, IOException {
//		if (instanceRepo.exists(stockCode, reportType, year, season) == false) {
//			return false;
//		}
//		if (dataRepo.exists(stockCode, reportType, year, season)) {
//			logExistsMsg(stockCode, reportType, year, season);
//			return false;
//		}
//		return true;
//	}
//
//	private FinancialReportData generateEntity(String stockCode,
//			ReportType reportType, Integer year, Integer season)
//			throws NoSuchFieldException, SecurityException,
//			IllegalArgumentException, IllegalAccessException,
//			NoSuchMethodException, InvocationTargetException,
//			InstantiationException, IOException {
//		FinancialReportInstance instance = instanceRepo.get(stockCode,
//				reportType, year, season);
//		FinancialReportData data = new FinancialReportData();
//		generateRowKeyContent(data, instance);
//		Date date = Calendar.getInstance().getTime();
//		generateItemFamilyContent(data, instance, date);
//		// generateRatioContent(data, date);
//		// generateGrowthContent(data, date);
//		return data;
//	}
//
//	private void generateRowKeyContent(FinancialReportData data,
//			FinancialReportInstance instance) {
//		byte[] rowKeyBytes = instance.getRowKey().toBytes();
//		data.new RowKey(rowKeyBytes, data);
//	}
//
//	private void generateItemFamilyContent(FinancialReportData data,
//			FinancialReportInstance instance, Date date) {
//		ItemFamily itemFam = data.getItemFamily();
//		InstanceFamily InstanceFam = instance.getInstanceFamily();
//		for (Entry<HBaseColumnQualifier, NavigableMap<Date, HBaseValue>> qualEnt : InstanceFam
//				.getQualifierVersionValueSet()) {
//			InstanceQualifier instanceQual = (InstanceQualifier) qualEnt
//					.getKey();
//			String elementId = instanceQual.getElementId();
//			String periodType = instanceQual.getPeriodType();
//			for (Entry<Date, HBaseValue> verEnt : qualEnt.getValue().entrySet()) {
//				InstanceValue instanceVal = (InstanceValue) verEnt.getValue();
//				BigDecimal value = getValue(instanceVal.getUnit(),
//						instanceVal.getValue());
//				if (DURATION.equals(periodType)) {
//					Date startDate = instanceQual.getStartDate();
//					Date endDate = instanceQual.getEndDate();
//					itemFam.add(elementId, date, periodType, startDate,
//							endDate, value);
//				} else if (INSTANT.equals(periodType)) {
//					Date instant = instanceQual.getInstant();
//					itemFam.add(elementId, date, periodType, instant, value);
//				} else {
//					throw new RuntimeException("PeriodType(" + periodType
//							+ ") not implement !!!");
//				}
//			}
//		}
//	}
//
//	private void generateRatioContent(FinancialReportData data, Date date) {
//		ItemFamily itemFam = data.getItemFamily();
//		RatioFamily ratioFam = data.getRatioFamily();
//		// TODO : continue implement.
//	}
//
//	private void generateGrowthContent(FinancialReportData data, Date date) {
//		ItemFamily itemFam = data.getItemFamily();
//		GrowthFamily growthFam = data.getGrowthFamily();
//		// TODO : continue implement.
//	}
//
//	private BigDecimal getValue(String unit, BigDecimal value) {
//		if (TWD.equals(unit)) {
//			return value;
//		} else if (SHARES.equals(unit)) {
//			return value;
//		} else {
//			throw new RuntimeException("Unit(" + unit + ") not implement !!!");
//		}
//	}
//
//	private void logExistsMsg(String stockCode, ReportType reportType,
//			Integer year, Integer season) {
//		logger.info("StockCode(" + stockCode + ") / reportType(" + reportType
//				+ ") / year(" + year + ") / season(" + season + ") already in "
//				+ dataRepo.getTargetTableName());
//	}
//
//	private void logSaveMsg(String stockCode, ReportType reportType,
//			Integer year, Integer season, FinancialReportData entity) {
//		RowKey rowKey = (RowKey) entity.getRowKey();
//		logger.info("StockCode(" + stockCode + ") / reportType(" + reportType
//				+ ") / year(" + year + ") / season(" + season + ") saved to "
//				+ rowKey.getTableName());
//	}
}

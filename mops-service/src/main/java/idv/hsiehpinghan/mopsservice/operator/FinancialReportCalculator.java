package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportData;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo.ReportTypeFamily.ReportTypeValue;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo.SeasonFamily.SeasonValue;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo.StockCodeFamily.StockCodeValue;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo.YearFamily.YearValue;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportDataRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportCalculator {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	@Autowired
	private FinancialReportDataRepository dataRepo;
	@Autowired
	private FinancialReportInstanceRepository instanceRepo;

	public void calculate(MopsDownloadInfo mopsDownloadInfo)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, IOException {
		String allStockCode = MopsDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
		String allReportType = MopsDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
		String allYear = MopsDownloadInfo.YearFamily.YearQualifier.ALL;
		String allSeason = MopsDownloadInfo.SeasonFamily.SeasonQualifier.ALL;

		StockCodeValue stockCodeValue = mopsDownloadInfo.getStockCodeFamily()
				.getLatestValue(allStockCode);
		ReportTypeValue reportTypeValue = mopsDownloadInfo
				.getReportTypeFamily().getLatestValue(allReportType);
		YearValue yearValue = mopsDownloadInfo.getYearFamily().getLatestValue(
				allYear);
		SeasonValue seasonValue = mopsDownloadInfo.getSeasonFamily()
				.getLatestValue(allSeason);

		for (String stockCode : stockCodeValue.getStockCodes()) {
			for (ReportType reportType : reportTypeValue.getReportTypes()) {
				for (Integer year : yearValue.getYears()) {
					for (Integer season : seasonValue.getSeasons()) {
						if (dataRepo
								.exists(stockCode, reportType, year, season)) {
							logExistsMsg(stockCode, reportType, year, season);
							continue;
						}
						FinancialReportData entity = generateEntity(stockCode, reportType, year, season);
						dataRepo.put(entity);
						
						return;
					}
				}
			}
		}
	}

	private FinancialReportData generateEntity(String stockCode,
			ReportType reportType, Integer year, Integer season)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, IOException {
		FinancialReportInstance instance = instanceRepo.get(stockCode,
				reportType, year, season);

		System.err.println(instance);
		
		return null;
	}

	private void logExistsMsg(String stockCode, ReportType reportType,
			Integer year, Integer season) {
		logger.info("StockCode(" + stockCode + ") / reportType(" + reportType
				+ ") / year(" + year + ") / season(" + season + ") already in "
				+ dataRepo.getTargetTableName());
	}
}

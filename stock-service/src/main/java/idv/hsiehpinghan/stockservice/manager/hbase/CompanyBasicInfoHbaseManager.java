package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.stockdao.entity.StockInfo;
import idv.hsiehpinghan.stockdao.entity.StockInfo.CompanyFamily;
import idv.hsiehpinghan.stockdao.enumeration.IndustryType;
import idv.hsiehpinghan.stockdao.enumeration.MarketType;
import idv.hsiehpinghan.stockdao.repository.StockInfoRepository;
import idv.hsiehpinghan.stockservice.manager.ICompanyBasicInfoManager;
import idv.hsiehpinghan.stockservice.operator.CompanyBasicInfoDownloader;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyBasicInfoHbaseManager implements ICompanyBasicInfoManager,
		InitializingBean {
	private final String[] EXTENSIONS = { "csv" };
	private final String BIG5 = "big5";
	private final String COMMA_STRING = StringUtility.COMMA_STRING;
	private final String EMPTY_STRING = StringUtility.EMPTY_STRING;
	private final String DOUBLE_UOTATION_STRING = StringUtility.DOUBLE_UOTATION_STRING;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File processedLog;

	@Autowired
	private StockServiceProperty stockServiceProperty;

	@Autowired
	private CompanyBasicInfoDownloader downloader;

	@Autowired
	private StockInfoRepository stockInfoRepo;

	// @Autowired
	// private IStockDownloadInfoRepository infoRepo;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = stockServiceProperty.getCompanyBasicInfoDownloadDir();
		generateProcessedLog();
	}

	@Override
	public synchronized boolean updateCompanyBasicInfo() {
		File dir = downloadCompanyBasicInfo();
		if (dir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveCompanyBasicInfoToHBase(dir);
			logger.info("Saved " + processFilesAmt + " files to "
					+ stockInfoRepo.getTargetTableName() + ".");
//			truncateProcessedLog();
		} catch (Exception e) {
			logger.error("Update company basic info fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	int saveCompanyBasicInfoToHBase(File dir) throws IOException,
			NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, NoSuchMethodException,
			InvocationTargetException, InstantiationException {
		int count = 0;
		Date ver = new Date();
		List<String> processedList = FileUtils.readLines(processedLog);
		// ex. otc_06.csv
		for (File file : FileUtils.listFiles(dir, EXTENSIONS, true)) {
			if (isProcessed(processedList, file)) {
				continue;
			}
			List<String> lines = FileUtils.readLines(file, BIG5);
			int startRow = getStartRow(file, lines);
			int size = lines.size();
			List<StockInfo> entities = new ArrayList<StockInfo>(size - startRow);
			// StockDownloadInfo downloadInfo = infoRepo
			// .getOrCreateEntity(comInfoRepo.getTargetTableName());
			String[] fnStrArr = file.getName().split("[_.]");
			MarketType marketType = MarketType.getMopsMarketType(fnStrArr[0]);
			IndustryType industryType = IndustryType
					.getMopsIndustryType(getString(fnStrArr[1]));
			for (int i = startRow; i < size; ++i) {
				String line = lines.get(i).replace(DOUBLE_UOTATION_STRING,
						EMPTY_STRING);
				String[] strArr = line.split(COMMA_STRING);
				if (strArr.length <= 1) {
					break;
				}
				String stockCode = getString(strArr[0]);
				// addStockCode(downloadInfo, now, stockCode);
				String chineseName = getString(strArr[1]);
				String englishBriefName = getString(strArr[24]);
				String unifiedBusinessNumber = getString(strArr[3]);
				String establishmentDate = getString(strArr[10]);
				String listingDate = getString(strArr[11]);
				String chairman = getString(strArr[4]);
				String generalManager = getString(strArr[5]);
				String spokesperson = getString(strArr[6]);
				String jobTitleOfSpokesperson = getString(strArr[7]);
				String actingSpokesman = getString(strArr[8]);
				String chineseAddress = getString(strArr[2]);
				String telephone = getString(strArr[9]);
				String stockTransferAgency = getString(strArr[18]);
				String telephoneOfStockTransferAgency = getString(strArr[19]);
				String addressOfStockTransferAgency = getString(strArr[20]);
				String englishAddress = getString(strArr[25]);
				String faxNumber = getString(strArr[26]);
				String email = getString(strArr[27]);
				String webSite = getString(strArr[28]);
				String financialReportType = getString(strArr[17]);
				String parValueOfOrdinaryShares = getString(strArr[12]);
				String paidInCapital = getString(strArr[13]);
				String amountOfOrdinaryShares = getString(strArr[14]);
				String privatePlacementAmountOfOrdinaryShares = getString(strArr[15]);
				String amountOfPreferredShares = getString(strArr[16]);
				String accountingFirm = getString(strArr[21]);
				String accountant1 = getString(strArr[22]);
				String accountant2 = getString(strArr[23]);
				StockInfo entity = generateEntity(stockCode, ver, marketType,
						industryType, chineseName, englishBriefName,
						unifiedBusinessNumber, establishmentDate, listingDate,
						chairman, generalManager, spokesperson,
						jobTitleOfSpokesperson, actingSpokesman,
						chineseAddress, telephone, stockTransferAgency,
						telephoneOfStockTransferAgency,
						addressOfStockTransferAgency, englishAddress,
						faxNumber, email, webSite, financialReportType,
						parValueOfOrdinaryShares, paidInCapital,
						amountOfOrdinaryShares,
						privatePlacementAmountOfOrdinaryShares,
						amountOfPreferredShares, accountingFirm, accountant1,
						accountant2);
				entities.add(entity);
			}
			stockInfoRepo.put(entities);
			logger.info(file.getName() + " saved to "
					+ stockInfoRepo.getTargetTableName() + ".");
			// infoRepo.put(downloadInfo);
			writeToProcessedFile(file);
			++count;
		}
		return count;
	}

	void truncateProcessedLog() throws IOException {
		FileUtils.write(processedLog, "", Charsets.UTF_8);
	}

	private String getTargetStartRowString(File file) {
		String fileName = file.getName();
		if (fileName.startsWith("sii")) {
			return "\"公司代號\",\"公司名稱\",\"住址\",\"營利事業統一編號\",\"董事長\",\"總經理\",\"發言人\",\"發言人職稱\",\"代理發言人\",\"總機電話\",\"成立日期\",\"上市日期\",\"普通股每股面額\",\"實收資本額(元)\",\"已發行普通股數或TDR原發行股數\",\"私募普通股(股)\",\"特別股(股)\",\"編製財務報告類型\",\"股票過戶機構\",\"過戶電話\",\"過戶地址\",\"簽證會計師事務所\",\"簽證會計師1\",\"簽證會計師2\",\"英文簡稱\",\"英文通訊地址\",\"傳真機號碼\",\"電子郵件信箱\",\"網址\"";
		} else if (fileName.startsWith("otc")) {
			return "\"公司代號\",\"公司名稱\",\"住址\",\"營利事業統一編號\",\"董事長\",\"總經理\",\"發言人\",\"發言人職稱\",\"代理發言人\",\"總機電話\",\"成立日期\",\"上櫃日期\",\"普通股每股面額\",\"實收資本額(元)\",\"已發行普通股數或TDR原發行股數\",\"私募普通股(股)\",\"特別股(股)\",\"編製財務報告類型\",\"股票過戶機構\",\"過戶電話\",\"過戶地址\",\"簽證會計師事務所\",\"簽證會計師1\",\"簽證會計師2\",\"英文簡稱\",\"英文通訊地址\",\"傳真機號碼\",\"電子郵件信箱\",\"網址\"";
		} else {
			throw new RuntimeException("Filename(" + file.getAbsolutePath()
					+ ") unexpected.");
		}
	}

	private int getStartRow(File file, List<String> lines) {
		String targetStr = getTargetStartRowString(file);
		for (int i = 0, size = lines.size(); i < size; ++i) {
			if (targetStr.equals(lines.get(i))) {
				return i + 1;
			}
		}
		throw new RuntimeException("File(" + file.getAbsolutePath()
				+ ") cannot find line(" + targetStr + ") !!!");
	}

	private String getString(String str) {
		return str.trim();
	}

	private boolean isProcessed(List<String> processedList, File file)
			throws IOException {
		String processedInfo = generateProcessedInfo(file);
		if (processedList.contains(processedInfo)) {
			logger.info(processedInfo + " processed before.");
			return true;
		}
		return false;
	}

	private String generateProcessedInfo(File file) {
		return file.getName();
	}

	private void writeToProcessedFile(File file) throws IOException {
		String infoLine = generateProcessedInfo(file) + System.lineSeparator();
		FileUtils.write(processedLog, infoLine, Charsets.UTF_8, true);
	}

	private StockInfo generateEntity(String stockCode, Date ver,
			MarketType marketType, IndustryType industryType,
			String chineseName, String englishBriefName,
			String unifiedBusinessNumber, String establishmentDate,
			String listingDate, String chairman, String generalManager,
			String spokesperson, String jobTitleOfSpokesperson,
			String actingSpokesman, String chineseAddress, String telephone,
			String stockTransferAgency, String telephoneOfStockTransferAgency,
			String addressOfStockTransferAgency, String englishAddress,
			String faxNumber, String email, String webSite,
			String financialReportType, String parValueOfOrdinaryShares,
			String paidInCapital, String amountOfOrdinaryShares,
			String privatePlacementAmountOfOrdinaryShares,
			String amountOfPreferredShares, String accountingFirm,
			String accountant1, String accountant2) {
		StockInfo entity = new StockInfo();
		entity.new RowKey(stockCode, entity);
		generateCompanyFamilyContent(entity, ver, marketType, industryType,
				chineseName, englishBriefName, unifiedBusinessNumber,
				establishmentDate, listingDate, chairman, generalManager,
				spokesperson, jobTitleOfSpokesperson, actingSpokesman,
				chineseAddress, telephone, stockTransferAgency,
				telephoneOfStockTransferAgency, addressOfStockTransferAgency,
				englishAddress, faxNumber, email, webSite, financialReportType,
				parValueOfOrdinaryShares, paidInCapital,
				amountOfOrdinaryShares, privatePlacementAmountOfOrdinaryShares,
				amountOfPreferredShares, accountingFirm, accountant1,
				accountant2);
		return entity;
	}

	private void generateCompanyFamilyContent(StockInfo entity, Date ver,
			MarketType marketType, IndustryType industryType,
			String chineseName, String englishBriefName,
			String unifiedBusinessNumber, String establishmentDate,
			String listingDate, String chairman, String generalManager,
			String spokesperson, String jobTitleOfSpokesperson,
			String actingSpokesman, String chineseAddress, String telephone,
			String stockTransferAgency, String telephoneOfStockTransferAgency,
			String addressOfStockTransferAgency, String englishAddress,
			String faxNumber, String email, String webSite,
			String financialReportType, String parValueOfOrdinaryShares,
			String paidInCapital, String amountOfOrdinaryShares,
			String privatePlacementAmountOfOrdinaryShares,
			String amountOfPreferredShares, String accountingFirm,
			String accountant1, String accountant2) {
		CompanyFamily fam = entity.getCompanyFamily();
		fam.setAccountant1(ver, accountant1);
		fam.setAccountant2(ver, accountant2);
		fam.setAccountingFirm(ver, accountingFirm);
		fam.setActingSpokesman(ver, actingSpokesman);
		fam.setAddressOfStockTransferAgency(ver, addressOfStockTransferAgency);
		fam.setAmountOfOrdinaryShares(ver, amountOfOrdinaryShares);
		fam.setAmountOfPreferredShares(ver, amountOfPreferredShares);
		fam.setChairman(ver, chairman);
		fam.setChineseAddress(ver, chineseAddress);
		fam.setChineseName(ver, chineseName);
		fam.setEmail(ver, email);
		fam.setEnglishAddress(ver, englishAddress);
		fam.setEnglishBriefName(ver, englishBriefName);
		fam.setEstablishmentDate(ver, establishmentDate);
		fam.setFaxNumber(ver, faxNumber);
		fam.setFinancialReportType(ver, financialReportType);
		fam.setGeneralManager(ver, generalManager);
		fam.setIndustryType(ver, industryType);
		fam.setJobTitleOfSpokesperson(ver, jobTitleOfSpokesperson);
		fam.setListingDate(ver, listingDate);
		fam.setMarketType(ver, marketType);
		fam.setPaidInCapital(ver, paidInCapital);
		fam.setParValueOfOrdinaryShares(ver, parValueOfOrdinaryShares);
		fam.setPrivatePlacementAmountOfOrdinaryShares(ver,
				privatePlacementAmountOfOrdinaryShares);
		fam.setSpokesperson(ver, spokesperson);
		fam.setStockTransferAgency(ver, stockTransferAgency);
		fam.setTelephone(ver, telephone);
		fam.setTelephoneOfStockTransferAgency(ver,
				telephoneOfStockTransferAgency);
		fam.setUnifiedBusinessNumber(ver, unifiedBusinessNumber);
		fam.setWebSite(ver, webSite);
	}

	// private void generateCommonFamilyContent(StockInfo entity, Date ver,
	// MarketType marketType, IndustryType industryType,
	// String chineseName, String englishBriefName,
	// String unifiedBusinessNumber, String establishmentDate,
	// String listingDate) {
	// CompanyFamily companyFamily = entity.getCompanyFamily();
	// companyFamily.setAccountant1(ver, accountant1);
	//
	//
	// companyFamily.setMarketType(ver, marketType);
	// companyFamily.setIndustryType(ver, industryType);
	// companyFamily.setChineseName(ver, chineseName);
	// commonFamily.add(CommonQualifier.CHINESE_NAME, now, chineseName);
	// commonFamily.add(CommonQualifier.ENGLISH_BRIEF_NAME, now,
	// englishBriefName);
	// commonFamily.add(CommonQualifier.UNIFIED_BUSINESS_NUMBER, now,
	// unifiedBusinessNumber);
	// commonFamily.add(CommonQualifier.ESTABLISHMENT_DATE, now,
	// establishmentDate);
	// commonFamily.add(CommonQualifier.LISTING_DATE, now, listingDate);
	// }

	// private void generateRoleFamilyContent(CompanyBasicInfo entity, Date now,
	// String chairman, String generalManager, String spokesperson,
	// String jobTitleOfSpokesperson, String actingSpokesman) {
	// RoleFamily roleFamily = entity.getRoleFamily();
	// roleFamily.add(RoleQualifier.CHAIRMAN, now, chairman);
	// roleFamily.add(RoleQualifier.GENERAL_MANAGER, now, generalManager);
	// roleFamily.add(RoleQualifier.SPOKESPERSON, now, spokesperson);
	// roleFamily.add(RoleQualifier.JOB_TITLE_OF_SPOKESPERSON_, now,
	// jobTitleOfSpokesperson);
	// roleFamily.add(RoleQualifier.ACTING_SPOKESMAN, now, actingSpokesman);
	// }
	//
	// private void generateCommunicationFamilyContent(CompanyBasicInfo entity,
	// Date now, String chineseAddress, String telephone,
	// String stockTransferAgency, String telephoneOfStockTransferAgency,
	// String addressOfStockTransferAgency, String englishAddress,
	// String faxNumber, String email, String webSite) {
	// CommunicationFamily communicationFamily = entity
	// .getCommunicationFamily();
	// communicationFamily.add(CommunicationQualifier.CHINESE_ADDRESS, now,
	// chineseAddress);
	// communicationFamily.add(CommunicationQualifier.TELEPHONE, now,
	// telephone);
	// communicationFamily.add(CommunicationQualifier.STOCK_TRANSFER_AGENCY,
	// now, stockTransferAgency);
	// communicationFamily.add(
	// CommunicationQualifier.TELEPHONE_OF_STOCK_TRANSFER_AGENCY, now,
	// telephoneOfStockTransferAgency);
	// communicationFamily.add(
	// CommunicationQualifier.ADDRESS_OF_STOCK_TRANSFER_AGENCY, now,
	// addressOfStockTransferAgency);
	// communicationFamily.add(CommunicationQualifier.ENGLISH_ADDRESS, now,
	// englishAddress);
	// communicationFamily.add(CommunicationQualifier.FAX_NUMBER, now,
	// faxNumber);
	// communicationFamily.add(CommunicationQualifier.EMAIL, now, email);
	// communicationFamily.add(CommunicationQualifier.WEB_SITE, now, webSite);
	// }
	//
	// private void generateFinanceFamilyContent(CompanyBasicInfo entity,
	// Date now, String financialReportType,
	// String parValueOfOrdinaryShares, String paidInCapital,
	// String amountOfOrdinaryShares,
	// String privatePlacementAmountOfOrdinaryShares,
	// String amountOfPreferredShares, String accountingFirm,
	// String accountant_1, String accountant_2) {
	// FinanceFamily financeFamily = entity.getFinanceFamily();
	// financeFamily.add(FinanceQualifier.FINANCIAL_REPORT_TYPE, now,
	// financialReportType);
	// financeFamily.add(FinanceQualifier.PAR_VALUE_OF_ORDINARY_SHARES, now,
	// parValueOfOrdinaryShares);
	// financeFamily.add(FinanceQualifier.PAID_IN_CAPITAL, now, paidInCapital);
	// financeFamily.add(FinanceQualifier.AMOUNT_OF_ORDINARY_SHARES, now,
	// amountOfOrdinaryShares);
	// financeFamily.add(
	// FinanceQualifier.PRIVATE_PLACEMENT_AMOUNT_OF_ORDINARY_SHARES,
	// now, privatePlacementAmountOfOrdinaryShares);
	// financeFamily.add(FinanceQualifier.AMOUNT_OF_PREFERRED_SHARES, now,
	// amountOfPreferredShares);
	// financeFamily
	// .add(FinanceQualifier.ACCOUNTING_FIRM, now, accountingFirm);
	// financeFamily.add(FinanceQualifier.ACCOUNTANT_1, now, accountant_1);
	// financeFamily.add(FinanceQualifier.ACCOUNTANT_2, now, accountant_2);
	// }

	private File downloadCompanyBasicInfo() {
		try {
			File dir = downloader.downloadCompanyBasicInfo();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return dir;
		} catch (Exception e) {
			logger.error("Download company basic info fail !!!");
			return null;
		}
	}

	private void generateProcessedLog() throws IOException {
		if (processedLog == null) {
			processedLog = new File(downloadDir, "processed.log");
			if (processedLog.exists() == false) {
				FileUtils.touch(processedLog);
			}
		}
	}

	// private void addStockCode(StockDownloadInfo downloadInfo, Date date,
	// String stockCode) {
	// String all = StockDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
	// downloadInfo.getStockCodeFamily().addStockCode(all, date, stockCode);
	// }
}

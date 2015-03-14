package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseColumnQualifier;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseValue;
import idv.hsiehpinghan.rassistant.assistant.RAssistant;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily.RatioDifferenceQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RatioDifferenceFamily.RatioDifferenceValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RowKey;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportAnalyzer {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private RAssistant rAssist;

	public void analyzeRatioDifference(File targetDirectory) throws IOException {
//		dataFrame <- read.csv("/tmp/getXbrlFromHbase/xbrl",colClasses=c("stockCode"="character","reportType"="factor","year"="integer","season"="integer","elementId"="character","periodType"="factor","instant"="Date","startDate"="Date","endDate"="Date","ratioDifference"="numeric"));
//		groups <- split(dataFrame, dataFrame$elementId);
//		test <- subset(dataFrame, subset=(year==2013 & season==4));
//		http://stackoverflow.com/questions/12127149/r-finding-the-max-date-for-each-id
//		http://www.statmethods.net/management/userfunctions.html
//		https://blog.udemy.com/r-tutorial/
	}
}

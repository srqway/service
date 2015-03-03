package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.stockdao.entity.Taxonomy.PresentationFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InfoFamily;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.repository.TaxonomyRepository;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportJsonMaker {
	private static final String COMMA_STRING = StringUtility.COMMA_STRING;
	// private Logger logger = Logger.getLogger(this.getClass().getName());
	private static final String YYYYMMDD = "yyyyMMdd";
	private static final String YYYY_MM_DD = "yyyy/MM/dd";
	private static final String DEEP = "deep";
	private static final String LABEL = "label";
	private static final String CHINESE_LABEL = "chinese_label";
	private static final String TITLE = "title";

	@Autowired
	private ObjectMapper objectMapper;
	// @Autowired
	// private XbrlRepository xbrlRepo;
	@Autowired
	private TaxonomyRepository taxoRepo;

	public ObjectNode getBalanceSheetJson(List<Xbrl> xbrls)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException,
			ParseException {
		if (xbrls.size() <= 0) {
			return null;
		}
		List<Date> instantDates = getMaxInstantDates(xbrls,
				Presentation.Id.BalanceSheet);
		ObjectNode targetNode = objectMapper.createObjectNode();
		generateInstantTitleNode(targetNode, instantDates);
		ObjectNode balanceSheetPresentTemplateNode = getBalanceSheetPresentTemplateNode(xbrls);
		generateInstantContent(targetNode, balanceSheetPresentTemplateNode,
				xbrls, Presentation.Id.BalanceSheet);

		System.err.println(targetNode);

		return targetNode;
	}

	public ObjectNode getStatementOfComprehensiveIncomeJson(List<Xbrl> xbrls)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException,
			ParseException {
		if (xbrls.size() <= 0) {
			return null;
		}
		Date[][] durationDatesArr = getMaxDurationDatesArr(xbrls,
				Presentation.Id.StatementOfComprehensiveIncome);
		ObjectNode targetNode = objectMapper.createObjectNode();
		generateDurationTitleNode(targetNode, durationDatesArr);
		ObjectNode statementOfComprehensiveIncomePresentTemplateNode = getStatementOfComprehensiveIncomeTemplateNode(xbrls);
		generateDurationContent(targetNode,
				statementOfComprehensiveIncomePresentTemplateNode, xbrls,
				Presentation.Id.StatementOfComprehensiveIncome);

		System.err.println(targetNode);

		return targetNode;
	}

	private void generateInstantTitleNode(ObjectNode targetNode,
			List<Date> instantDates) {
		ObjectNode titleNode = objectMapper.createObjectNode();
		for (Date instantDate : instantDates) {
			String key = getInstantTitleKey(instantDate);
			String value = DateFormatUtils.format(instantDate, YYYY_MM_DD);
			titleNode.put(key, value);
		}
		targetNode.set(TITLE, titleNode);
	}

	private void generateDurationTitleNode(ObjectNode targetNode,
			Date[][] durationDatesArr) {
		ObjectNode titleNode = objectMapper.createObjectNode();
		for (Date[] dates : durationDatesArr) {
			Date startDate = dates[0];
			Date endDate = dates[1];
			String key = getDurationTitleKey(startDate, endDate);
			String value = DateFormatUtils.format(startDate, YYYY_MM_DD)
					+ " ~ " + DateFormatUtils.format(endDate, YYYY_MM_DD);
			titleNode.put(key, value);
		}
		targetNode.set(TITLE, titleNode);
	}

	private String getInstantTitleKey(Date instantDate) {
		return DateFormatUtils.format(instantDate, YYYYMMDD);
	}

	private String getDurationTitleKey(Date startDate, Date endDate) {
		return DateFormatUtils.format(startDate, YYYYMMDD) + "~"
				+ DateFormatUtils.format(endDate, YYYYMMDD);
	}

	private String[] getPeriods(InfoFamily infoFam, String presentId) {
		switch (presentId) {
		case Presentation.Id.BalanceSheet:
			return infoFam.getBalanceSheetContext().split(COMMA_STRING);
		case Presentation.Id.StatementOfComprehensiveIncome:
			return infoFam.getStatementOfComprehensiveIncomeContext().split(
					COMMA_STRING);
		case Presentation.Id.StatementOfCashFlows:
			return infoFam.getStatementOfCashFlowsContext().split(COMMA_STRING);
		case Presentation.Id.StatementOfChangesInEquity:
			return infoFam.getStatementOfChangesInEquityContext().split(
					COMMA_STRING);
		default:
			throw new RuntimeException("Present id(" + presentId
					+ ") not implements !!!");
		}
	}

	private List<Date> getMaxInstantDates(List<Xbrl> xbrls, String presentId)
			throws ParseException {
		List<Date> dates = new ArrayList<Date>(xbrls.size());
		for (Xbrl xbrl : xbrls) {
			String[] periods = getPeriods(xbrl.getInfoFamily(), presentId);
			Date maxInstantDate = getMaxInstantDate(periods);
			dates.add(maxInstantDate);
		}
		return dates;
	}

	private Date[][] getMaxDurationDatesArr(List<Xbrl> xbrls, String presentId)
			throws ParseException {
		int size = xbrls.size();
		Date[][] datesArr = new Date[size][2];
		for (int i = 0; i < size; ++i) {
			Xbrl xbrl = xbrls.get(i);
			String[] periods = getPeriods(xbrl.getInfoFamily(), presentId);
			datesArr[i] = getMaxDurationDates(periods);
		}
		return datesArr;
	}

	private void generateInstantContent(ObjectNode targetNode,
			ObjectNode balanceSheetPresentTemplateNode, List<Xbrl> xbrls,
			String presentId) throws ParseException {
		generateContentNode(PeriodType.INSTANT, targetNode,
				balanceSheetPresentTemplateNode, xbrls, presentId, 0);
	}

	private void generateDurationContent(ObjectNode targetNode,
			ObjectNode statementOfComprehensiveIncomePresentTemplateNode,
			List<Xbrl> xbrls, String presentId) throws ParseException {
		generateContentNode(PeriodType.DURATION, targetNode,
				statementOfComprehensiveIncomePresentTemplateNode, xbrls,
				presentId, 0);
	}

	private Date getInstantDate(String period) throws ParseException {
		return DateUtils.parseDate(period, YYYYMMDD);
	}

	private List<Date> getInstantDates(String[] periods) throws ParseException {
		List<Date> dates = new ArrayList<Date>(periods.length);
		for (String period : periods) {
			dates.add(getInstantDate(period));
		}
		return dates;
	}

	private Date[][] getDurationDatesArr(String[] periods)
			throws ParseException {
		int size = periods.length;
		Date[][] datesArr = new Date[size][2];
		for (int i = 0; i < size; ++i) {
			String[] dates = periods[i].split("~");
			datesArr[i][0] = DateUtils.parseDate(dates[0], YYYYMMDD);
			datesArr[i][1] = DateUtils.parseDate(dates[1], YYYYMMDD);
		}
		return datesArr;
	}

	private Date getMaxInstantDate(String[] periods) throws ParseException {
		List<Date> dates = getInstantDates(periods);
		Date maxInstantDate = null;
		for (Date date : dates) {
			if (maxInstantDate == null) {
				maxInstantDate = date;
			} else {
				if (maxInstantDate.getTime() < date.getTime()) {
					maxInstantDate = date;
				}
			}
		}
		return maxInstantDate;
	}

	private Date[] getMaxDurationDates(String[] periods) throws ParseException {
		Date[][] datesArr = getDurationDatesArr(periods);
		Date[] maxStartDateAndEndDate = null;
		for (Date[] dates : datesArr) {
			if (maxStartDateAndEndDate == null) {
				maxStartDateAndEndDate = dates;
			} else {
				if ((maxStartDateAndEndDate[0].getTime() < dates[0].getTime())
						&& (maxStartDateAndEndDate[1].getTime() < dates[1]
								.getTime())) {
					maxStartDateAndEndDate = dates;
				}
			}
		}
		return maxStartDateAndEndDate;
	}

	private boolean generateContentNode(PeriodType periodType,
			ObjectNode targetNode, ObjectNode presentTemplateNode,
			List<Xbrl> xbrls, String presentId, int deep) throws ParseException {
		boolean hasContent = false;
		Iterator<Map.Entry<String, JsonNode>> iter = presentTemplateNode
				.fields();
		while (iter.hasNext()) {
			// Reset hasContent.
			hasContent = false;
			Map.Entry<String, JsonNode> ent = iter.next();
			String key = ent.getKey();
			JsonNode node = ent.getValue();
			if (node.isObject() == false) {
				continue;
			}
			ObjectNode objNode = objectMapper.createObjectNode();
			targetNode.set(key, objNode);
			objNode.put(DEEP, deep);
			objNode.put(LABEL, node.get(CHINESE_LABEL).asText());
			for (Xbrl xbrl : xbrls) {
				InfoFamily infoFam = xbrl.getInfoFamily();
				if (PeriodType.INSTANT.equals(periodType)) {;
					Date instant = getMaxInstantDate(getPeriods(infoFam, presentId));
					BigDecimal ratio = xbrl.getGrowthFamily().getRatio(key,
							periodType, instant);
					if (ratio == null) {
						continue;
					}
					objNode.put(getInstantTitleKey(instant), ratio);
					hasContent |= true;

				} else if (PeriodType.DURATION.equals(periodType)) {
					Date[] dates = getMaxDurationDates(getPeriods(infoFam,
							presentId));
					Date startDate = dates[0];
					Date endDate = dates[1];
					BigDecimal ratio = xbrl.getGrowthFamily().getRatio(key,
							periodType, startDate, endDate);
					if (ratio == null) {
						continue;
					}
					objNode.put(getDurationTitleKey(startDate, endDate), ratio);
					hasContent |= true;

				} else {
					throw new RuntimeException("Period type(" + periodType
							+ ") not implement !!!");
				}
			}
			generateContentNode(periodType, targetNode, (ObjectNode) node,
					xbrls, presentId, deep + 1);
			if (hasContent == false) {
				targetNode.remove(key);
			}
		}

		return false;
	}

	// private boolean generateContentNode(PeriodType periodType,
	// ObjectNode targetNode, ObjectNode presentTemplateNode, Xbrl xbrl,
	// int deep) {
	// boolean hasContent = false;
	// Iterator<Map.Entry<String, JsonNode>> iter = presentTemplateNode
	// .fields();
	// while (iter.hasNext()) {
	// // Reset hasContent.
	// hasContent = false;
	// Map.Entry<String, JsonNode> ent = iter.next();
	// String key = ent.getKey();
	// JsonNode node = ent.getValue();
	// if (node.isObject()) {
	// ObjectNode objNode = objectMapper.createObjectNode();
	// targetNode.set(key, objNode);
	// objNode.put(DEEP, deep);
	// objNode.put(LABEL, node.get(CHINESE_LABEL).asText());
	//
	// if (PeriodType.INSTANT.equals(periodType)) {
	// for (String period : periods) {
	// BigDecimal itemValue = getInstantItemValue(key, period,
	// xbrl);
	// if (itemValue == null) {
	// continue;
	// }
	// objNode.put(period, itemValue);
	// hasContent |= true;
	// }
	// } else if (PeriodType.DURATION.equals(periodType)) {
	// for (String period : periods) {
	// BigDecimal itemValue = getDurationItemValue(key,
	// period, xbrl);
	// if (itemValue == null) {
	// continue;
	// }
	// objNode.put(period, itemValue);
	// hasContent |= true;
	// }
	// } else {
	// throw new RuntimeException("Period type(" + periodType
	// + ") not implement !!!");
	// }
	// hasContent |= generateContentNode((ObjectNode) node,
	// targetNode, presentId, periodType, periods, xbrl,
	// deep + 1);
	// if (hasContent == false) {
	// targetNode.remove(key);
	// }
	// }
	// }
	// return hasContent;
	// }

	private ObjectNode getBalanceSheetPresentTemplateNode(List<Xbrl> xbrls)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		String balanceSheetPresentJsonStr = getPresentationFamily(xbrls)
				.getBalanceSheet();
		return (ObjectNode) objectMapper.readTree(balanceSheetPresentJsonStr);
	}

	private ObjectNode getStatementOfComprehensiveIncomeTemplateNode(
			List<Xbrl> xbrls) throws IllegalAccessException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		String statementOfComprehensiveIncomePresentJsonStr = getPresentationFamily(
				xbrls).getStatementOfComprehensiveIncome();
		return (ObjectNode) objectMapper
				.readTree(statementOfComprehensiveIncomePresentJsonStr);
	}

	private PresentationFamily getPresentationFamily(List<Xbrl> xbrls)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		InfoFamily infoFam = getLatestEntity(xbrls).getInfoFamily();
		return taxoRepo.get(infoFam.getVersion()).getPresentationFamily();
	}

	private Xbrl getLatestEntity(List<Xbrl> xbrls) {
		return xbrls.get(xbrls.size() - 1);
	}

	// public ObjectNode getBalanceSheetJson(String stockCode,
	// ReportType reportType) throws ParseException,
	// IllegalAccessException, NoSuchMethodException, SecurityException,
	// InstantiationException, IllegalArgumentException,
	// InvocationTargetException, IOException {
	// List<Xbrl> xbrls = xbrlRepo
	// .fuzzyScan(stockCode, reportType, null, null);
	// if (xbrls.size() <= 0) {
	// return null;
	// }
	// List<Date> instantDates = getMaxInstantDates(xbrls);
	// ObjectNode targetNode = objectMapper.createObjectNode();
	// generateInstantTitleNode(targetNode, instantDates);
	// ObjectNode balanceSheetPresentTemplateNode =
	// getBalanceSheetPresentTemplateNode(xbrls);
	//
	// for (Xbrl xbrl : xbrls) {
	// generateContentNodes(targetNode, balanceSheetPresentTemplateNode,
	// xbrl);
	// }
	//
	// System.err.println(targetNode);
	//
	// return null;
	// }

	// private String getInstantPeriodTitle(Date instantDate) throws
	// ParseException {
	// return DateFormatUtils.format(instantDate, YYYY_MM_DD);
	// }

	// private ObjectNode getBalanceSheetPresentTemplateNode(List<Xbrl> xbrls)
	// throws IllegalAccessException, NoSuchMethodException, SecurityException,
	// InstantiationException, IllegalArgumentException,
	// InvocationTargetException, IOException {
	// String balanceSheetPresentJsonStr = getPresentationJsonStr(
	// getLatestEntity(xbrls), Presentation.Id.BalanceSheet);
	// return (ObjectNode) objectMapper.readTree(balanceSheetPresentJsonStr);
	// }
	//

	//
	// private BigDecimal getInstantItemValue(String elementId, Date instant,
	// Xbrl xbrl) throws ParseException {
	// return xbrl.getItemFamily().get(elementId, PeriodType.INSTANT, instant);
	// }
	//
	// private BigDecimal getDurationItemValue(String elementId, Date startDate,
	// Date endDate,
	// Xbrl xbrl) throws ParseException {
	// return xbrl.getItemFamily().get(elementId, PeriodType.DURATION,
	// startDate, endDate);
	// }
	//
	// private Date getMaxInstantDate(InfoFamily infoFam) throws ParseException
	// {
	// List<Date> dates = getInstantDates(infoFam);
	// Date maxInstantDate = null;
	// for (Date date : dates) {
	// if (maxInstantDate == null) {
	// maxInstantDate = date;
	// } else {
	// if (maxInstantDate.getTime() < date.getTime()) {
	// maxInstantDate = date;
	// }
	// }
	// }
	// return maxInstantDate;
	// }
	//
	// private List<Date> getInstantDates(InfoFamily infoFam)
	// throws ParseException {
	// String[] periods = infoFam.getBalanceSheetContext().split(COMMA_STRING);
	// List<Date> dates = new ArrayList<Date>(periods.length);
	// for (String period : periods) {
	// dates.add(getInstantDate(period));
	// }
	// return dates;
	// }

	// private ObjectNode generateBalanceSheetJsonObject(ObjectNode targetNode,
	// String balanceSheetPresentJsonStr, Xbrl xbrl, Date instantDate) {
	// ObjectNode srcNode = (ObjectNode) objectMapper
	// .readTree(balanceSheetPresentJsonStr);
	// generateContentNodes(srcNode, targetNode, presentId, periodType,
	// periods, xbrl);
	// return targetNode;
	// }

	private String getPresentationJsonStr(Xbrl xbrl, String presentId)
			throws IllegalAccessException, NoSuchMethodException,
			SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException {
		InfoFamily infoFam = xbrl.getInfoFamily();
		PresentationFamily presentFamily = taxoRepo.get(infoFam.getVersion())
				.getPresentationFamily();
		switch (presentId) {
		case Presentation.Id.BalanceSheet:
			return presentFamily.getBalanceSheet();
		case Presentation.Id.StatementOfComprehensiveIncome:
			return presentFamily.getStatementOfComprehensiveIncome();
		case Presentation.Id.StatementOfCashFlows:
			return presentFamily.getStatementOfCashFlows();
		case Presentation.Id.StatementOfChangesInEquity:
			return presentFamily.getStatementOfChangesInEquity();
		default:
			throw new RuntimeException("Presentation id(" + presentId
					+ ") not implemented !!!");
		}
	}

	// public Map<String, ObjectNode> getPresentationJsonMap(
	// List<String> presentIds, String stockCode, ReportType reportType,
	// Integer year, Integer season) throws IllegalAccessException,
	// NoSuchMethodException, SecurityException, InstantiationException,
	// IllegalArgumentException, InvocationTargetException, IOException,
	// NoSuchFieldException, ParseException {
	// if (xbrlRepo.exists(stockCode, reportType, year, season) == false) {
	// return null;
	// }
	// Xbrl xbrl = xbrlRepo.get(stockCode, reportType, year, season);
	// InfoFamily infoFam = xbrl.getInfoFamily();
	// PresentationFamily presentFamily = taxoRepo.get(infoFam.getVersion())
	// .getPresentationFamily();
	//
	// Map<String, ObjectNode> map = new HashMap<String, ObjectNode>(
	// presentIds.size());
	// for (String presentId : presentIds) {
	// ObjectNode objNode = null;
	// if (Presentation.Id.BalanceSheet.equals(presentId)) {
	// String jsonStr = presentFamily.getBalanceSheet();
	// String[] periods = infoFam.getBalanceSheetContext().split(
	// COMMA_STRING);
	// objNode = generateJsonObject(presentId, jsonStr,
	// PeriodType.INSTANT, periods, xbrl);
	// } else if (Presentation.Id.StatementOfComprehensiveIncome
	// .equals(presentId)) {
	// String jsonStr = presentFamily
	// .getStatementOfComprehensiveIncome();
	// String[] periods = infoFam
	// .getStatementOfComprehensiveIncomeContext().split(
	// COMMA_STRING);
	// objNode = generateJsonObject(presentId, jsonStr,
	// PeriodType.DURATION, periods, xbrl);
	// } else if (Presentation.Id.StatementOfCashFlows.equals(presentId)) {
	// String jsonStr = presentFamily.getStatementOfCashFlows();
	// String[] periods = infoFam.getStatementOfCashFlowsContext()
	// .split(COMMA_STRING);
	// objNode = generateJsonObject(presentId, jsonStr,
	// PeriodType.DURATION, periods, xbrl);
	// } else if (Presentation.Id.StatementOfChangesInEquity
	// .equals(presentId)) {
	// String jsonStr = presentFamily.getStatementOfChangesInEquity();
	// String[] periods = infoFam
	// .getStatementOfChangesInEquityContext().split(
	// COMMA_STRING);
	// objNode = generateJsonObject(presentId, jsonStr,
	// PeriodType.DURATION, periods, xbrl);
	// } else {
	// throw new RuntimeException("Presentation id(" + presentId
	// + ") not implements !!!");
	// }
	// map.put(presentId, objNode);
	// }
	// return map;
	// }

	// private ObjectNode generateJsonObject(String presentId, String
	// jsonString,
	// PeriodType periodType, String[] periods, Xbrl xbrl)
	// throws JsonProcessingException, IOException, ParseException {
	// ObjectNode srcNode = (ObjectNode) objectMapper.readTree(jsonString);
	// ObjectNode targetNode = objectMapper.createObjectNode();
	// generateTitleNode(targetNode, periodType, periods);
	// generateContentNodes(srcNode, targetNode, presentId, periodType,
	// periods, xbrl);
	// return targetNode;
	// }

	// private void generateTitleNode(ObjectNode targetNode,
	// PeriodType periodType, String[] periods) throws ParseException {
	// ObjectNode titleNode = objectMapper.createObjectNode();
	// if (PeriodType.INSTANT.equals(periodType)) {
	// for (String period : periods) {
	// titleNode.put(period, getInstantPeriodTitle(period));
	// }
	// } else if (PeriodType.DURATION.equals(periodType)) {
	// for (String period : periods) {
	// titleNode.put(period, getDurationPeriodTitle(period));
	// }
	// } else {
	// throw new RuntimeException("PeriodType(" + periodType
	// + ") not implements !!!");
	// }
	//
	// targetNode.set(TITLE, titleNode);
	// }
	//
	// private String getInstantPeriodTitle(String period) throws ParseException
	// {
	// Date date = getInstantDate(period);
	// return DateFormatUtils.format(date, YYYY_MM_DD);
	// }
	//
	// private String getDurationPeriodTitle(String period) throws
	// ParseException {
	// Date startDate = getStartDate(period);
	// Date endDate = getEndDate(period);
	// return DateFormatUtils.format(startDate, YYYY_MM_DD) + " ~ "
	// + DateFormatUtils.format(endDate, YYYY_MM_DD);
	// }
	//
	//
	// private Date getStartDate(String period) throws ParseException {
	// String[] dates = period.split("~");
	// return DateUtils.parseDate(dates[0], YYYYMMDD);
	// }
	//
	// private Date getEndDate(String period) throws ParseException {
	// String[] dates = period.split("~");
	// return DateUtils.parseDate(dates[1], YYYYMMDD);
	// }
}

package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportData;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportData.ItemFamily.ItemValue;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance.InfoFamily;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportPresentation;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportDataRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Instance;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportJsonMaker {
	// private Logger logger = Logger.getLogger(this.getClass().getName());
	private static final String YYYYMMDD = "yyyyMMdd";
	private static final String YYYY_MM_DD = "yyyy/MM/dd";
	private static final String DEEP = "deep";
	private static final String LABEL = "label";
	private static final String CHINESE_LABEL = "chinese_label";
	public static final String TITLE = "title";
	@Autowired
	private FinancialReportPresentationRepository presentRepo;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private FinancialReportInstanceRepository instanceRepo;
	@Autowired
	private FinancialReportDataRepository dataRepo;

	public Map<String, ObjectNode> getPresentationJsonMap(
			List<String> presentIds, String stockCode, ReportType reportType,
			Integer year, Integer season) throws IllegalAccessException,
			NoSuchMethodException, SecurityException, InstantiationException,
			IllegalArgumentException, InvocationTargetException, IOException,
			NoSuchFieldException, ParseException {
		InfoFamily infoFam = instanceRepo.get(stockCode, reportType, year,
				season).getInfoFamily();
		FinancialReportPresentation present = presentRepo
				.get(getTaxonomyVersion(infoFam));
		FinancialReportData dateEntity = dataRepo.get(stockCode, reportType,
				year, season);
		// InstanceFamily instanceFam = instance.getInstanceFamily();
		Map<String, ObjectNode> map = new HashMap<String, ObjectNode>(
				presentIds.size());
		for (String presentId : presentIds) {
			String jsonStr = present.getJsonFamily().getValue(presentId)
					.getJson();
			ObjectNode objNode = null;
			if (Presentation.Id.BalanceSheet.equals(presentId)) {
				String[] periods = getPeriods(infoFam, presentId,
						Instance.Attribute.INSTANT);
				objNode = generateJsonObject(presentId, jsonStr,
						Instance.Attribute.INSTANT, periods, dateEntity);
			} else if (Presentation.Id.StatementOfComprehensiveIncome
					.equals(presentId)) {
				String[] periods = getPeriods(infoFam, presentId,
						Instance.Attribute.DURATION);
				objNode = generateJsonObject(presentId, jsonStr,
						Instance.Attribute.DURATION, periods, dateEntity);
			} else if (Presentation.Id.StatementOfCashFlows.equals(presentId)) {
				String[] periods = getPeriods(infoFam, presentId,
						Instance.Attribute.DURATION);
				objNode = generateJsonObject(presentId, jsonStr,
						Instance.Attribute.DURATION, periods, dateEntity);
			} else if (Presentation.Id.StatementOfChangesInEquity
					.equals(presentId)) {
				String[] periods = getPeriods(infoFam, presentId,
						Instance.Attribute.DURATION);
				objNode = generateJsonObject(presentId, jsonStr,
						Instance.Attribute.DURATION, periods, dateEntity);
			} else {
				throw new RuntimeException("Presentation id(" + presentId
						+ ") not implements !!!");
			}
			map.put(presentId, objNode);
		}
		return map;
	}

	private String[] getPeriods(InfoFamily infoFamily, String presentationId,
			String periodType) {
		String periods = infoFamily.getLatestValue(presentationId, periodType)
				.getInfoContent();
		return periods.split(",");
	}

	private XbrlTaxonomyVersion getTaxonomyVersion(InfoFamily infoFamily) {
		String version = infoFamily.getLatestValue(InstanceAssistant.VERSION)
				.getInfoContent();
		return XbrlTaxonomyVersion.valueOf(version);
	}

	private ObjectNode generateJsonObject(String presentId, String jsonString,
			String periodType, String[] periods, FinancialReportData dateEntity)
			throws JsonProcessingException, IOException, ParseException {
		ObjectNode srcNode = (ObjectNode) objectMapper.readTree(jsonString);
		ObjectNode targetNode = objectMapper.createObjectNode();
		generateTitleNode(targetNode, periodType, periods);
		generateContentNodes(srcNode, targetNode, presentId, periodType,
				periods, dateEntity);
		return targetNode;
	}

	private void generateTitleNode(ObjectNode targetNode, String periodType,
			String[] periods) throws ParseException {
		ObjectNode titleNode = objectMapper.createObjectNode();
		if (Instance.Attribute.INSTANT.equals(periodType)) {
			for (String period : periods) {
				titleNode.put(period, getInstantPeriodTitle(period));
			}
		} else if (Instance.Attribute.DURATION.equals(periodType)) {
			for (String period : periods) {
				titleNode.put(period, getDurationPeriodTitle(period));
			}
		} else {
			throw new RuntimeException("PeriodType(" + periodType
					+ ") not implements !!!");
		}

		targetNode.set(TITLE, titleNode);
	}

	private String getInstantPeriodTitle(String period) throws ParseException {
		Date date = getInstantDate(period);
		return DateFormatUtils.format(date, YYYY_MM_DD);
	}

	private String getDurationPeriodTitle(String period) throws ParseException {
		Date startDate = getStartDate(period);
		Date endDate = getEndDate(period);
		return DateFormatUtils.format(startDate, YYYY_MM_DD) + " ~ "
				+ DateFormatUtils.format(endDate, YYYY_MM_DD);
	}

	private void generateContentNodes(ObjectNode srcNode,
			ObjectNode targetNode, String presentId, String periodType,
			String[] periods, FinancialReportData dateEntity)
			throws ParseException {
		generateContentNode(srcNode, targetNode, presentId, periodType,
				periods, dateEntity, 0);
	}

	private boolean generateContentNode(ObjectNode srcNode,
			ObjectNode targetNode, String presentId, String periodType,
			String[] periods, FinancialReportData dateEntity, int deep)
			throws ParseException {
		boolean hasContent = false;
		Iterator<Map.Entry<String, JsonNode>> iter = srcNode.fields();
		while (iter.hasNext()) {
			// Reset hasContent.
			hasContent = false;
			Map.Entry<String, JsonNode> ent = iter.next();
			String key = ent.getKey();
			JsonNode node = ent.getValue();
			if (node.isObject()) {
				ObjectNode objNode = objectMapper.createObjectNode();
				targetNode.set(key, objNode);
				objNode.put(DEEP, deep);
				objNode.put(LABEL, node.get(CHINESE_LABEL).asText());
				if (Instance.Attribute.INSTANT.equals(periodType)) {
					for (String period : periods) {
						ItemValue itemValue = getInstantItemValue(key, period,
								dateEntity);
						if (itemValue == null) {
							continue;
						}
						objNode.put(period, itemValue.getValue());
						hasContent |= true;
					}
				} else if (Instance.Attribute.DURATION.equals(periodType)) {
					for (String period : periods) {
						ItemValue itemValue = getDurationItemValue(key, period,
								dateEntity);
						if (itemValue == null) {
							continue;
						}
						objNode.put(period, itemValue.getValue());
						hasContent |= true;
					}
				} else {
					throw new RuntimeException("Period type(" + periodType
							+ ") not implement !!!");
				}
				hasContent |= generateContentNode((ObjectNode) node,
						targetNode, presentId, periodType, periods, dateEntity,
						deep + 1);
				if(hasContent == false) {
					targetNode.remove(key);	
				}
			}
		}
		return hasContent;
	}

	private ItemValue getInstantItemValue(String elementId, String period,
			FinancialReportData dateEntity) throws ParseException {
		Date instant = DateUtils.parseDate(period, YYYYMMDD);
		return dateEntity.getItemFamily().getLatestValue(elementId,
				Instance.Attribute.INSTANT, instant);
	}

	private ItemValue getDurationItemValue(String elementId, String period,
			FinancialReportData dateEntity) throws ParseException {
		Date startDate = getStartDate(period);
		Date endDate = getEndDate(period);
		return dateEntity.getItemFamily().getLatestValue(elementId,
				Instance.Attribute.DURATION, startDate, endDate);
	}

	private Date getInstantDate(String period) throws ParseException {
		return DateUtils.parseDate(period, YYYYMMDD);
	}

	private Date getStartDate(String period) throws ParseException {
		String[] dates = period.split("~");
		return DateUtils.parseDate(dates[0], YYYYMMDD);
	}

	private Date getEndDate(String period) throws ParseException {
		String[] dates = period.split("~");
		return DateUtils.parseDate(dates[1], YYYYMMDD);
	}
}

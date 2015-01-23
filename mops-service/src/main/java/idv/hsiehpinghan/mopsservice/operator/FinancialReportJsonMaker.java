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

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportJsonMaker {
	// private Logger logger = Logger.getLogger(this.getClass().getName());
	private static final String YYYYMMDD = "yyyyMMdd";
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
		generateTitleNode(targetNode, periods);
		generateContentNodes(srcNode, targetNode, presentId, periodType,
				periods, dateEntity);
		return targetNode;
	}

	private void generateTitleNode(ObjectNode targetNode, String[] periods) {
		ArrayNode titleNode = objectMapper.createArrayNode();
		for (String period : periods) {
			titleNode.add(period);
		}
		targetNode.set(TITLE, titleNode);
	}

	private void generateContentNodes(ObjectNode srcNode,
			ObjectNode targetNode, String presentId, String periodType,
			String[] periods, FinancialReportData dateEntity)
			throws ParseException {
		generateContentNode(srcNode, targetNode, presentId, periodType,
				periods, dateEntity, 0);
	}

	private void generateContentNode(ObjectNode srcNode, ObjectNode targetNode,
			String presentId, String periodType, String[] periods,
			FinancialReportData dateEntity, int deep) throws ParseException {
		Iterator<Map.Entry<String, JsonNode>> iter = srcNode.fields();
		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> ent = iter.next();
			String key = ent.getKey();
			JsonNode node = ent.getValue();
			if (node.isObject()) {
				ArrayNode valuesNode = objectMapper.createArrayNode();
				// ObjectNode objNode = (ObjectNode) node;
				String chineseLabel = node.get(CHINESE_LABEL).asText();
				if (Instance.Attribute.INSTANT.equals(periodType)) {
					for (String period : periods) {
						ItemValue itemValue = getInstantItemValue(key, period,
								dateEntity);
						if (itemValue == null) {
							continue;
						}
						valuesNode.add(itemValue.getValue());
					}
				} else if (Instance.Attribute.DURATION.equals(periodType)) {
					for (String period : periods) {
						ItemValue itemValue = getDurationItemValue(key, period,
								dateEntity);
						if (itemValue == null) {
							continue;
						}
						valuesNode.add(itemValue.getValue());
					}
				} else {
					throw new RuntimeException("Period type(" + periodType
							+ ") not implement !!!");
				}
				if (valuesNode.size() > 0) {
					targetNode.set(chineseLabel, valuesNode);
				}
				generateContentNode((ObjectNode) node, targetNode, presentId,
						periodType, periods, dateEntity, deep);
			}
		}
	}

	private ItemValue getInstantItemValue(String elementId, String period,
			FinancialReportData dateEntity) throws ParseException {
		Date instant = DateUtils.parseDate(period, YYYYMMDD);
		return dateEntity.getItemFamily().getLatestValue(elementId,
				Instance.Attribute.INSTANT, instant);
	}

	private ItemValue getDurationItemValue(String elementId, String period,
			FinancialReportData dateEntity) throws ParseException {
		String[] dates = period.split("~");
		Date startDate = DateUtils.parseDate(dates[0], YYYYMMDD);
		Date endDate = DateUtils.parseDate(dates[1], YYYYMMDD);
		return dateEntity.getItemFamily().getLatestValue(elementId,
				Instance.Attribute.DURATION, startDate, endDate);
	}
}

package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datatypeutility.utility.BigDecimalUtility;
import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseColumnQualifier;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.GrowthFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InfoFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily.InstanceQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.ItemFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl.ItemFamily.ItemQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.ItemFamily.ItemValue;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.enumeration.UnitType;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Instance;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class XbrlInstanceConverter {
	private static final String COMMA_STRING = StringUtility.COMMA_STRING;
	private static final String DATE_PATTERN = "yyyyMMdd";
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private XbrlRepository xbrlRepo;

	public Xbrl convert(String stockCode, ReportType reportType, int year,
			int season, ObjectNode objNode) throws ParseException {
		Xbrl entity = xbrlRepo.generateEntity(stockCode, reportType, year,
				season);
		generateRowKey(entity, stockCode, reportType, year, season);
		generateColumnFamilies(entity, objNode);
		return entity;
	}

	private void generateColumnFamilies(Xbrl entity, ObjectNode objNode)
			throws ParseException {
		Date ver = Calendar.getInstance().getTime();
		generateInfoFamily(entity, objNode, ver);
		generateInstanceFamily(entity, objNode, ver);
		generateItemFamily(entity, ver);
		generateGrowthFamily(entity, ver);
	}

	private Date getDate(JsonNode dateNode) throws ParseException {
		if (dateNode == null) {
			return null;
		}
		return DateUtils.parseDate(dateNode.textValue(), DATE_PATTERN);
	}

	private BigDecimal getTwdValue(UnitType unitType, BigDecimal value) {
		if (UnitType.TWD.equals(unitType)) {
			return value;
		} else if (UnitType.SHARES.equals(unitType)) {
			return value;
		} else {
			throw new RuntimeException("UnitType(" + unitType
					+ ") not implement !!!");
		}
	}

	private BigDecimal getOneYearBeforeValue(ItemFamily itemFamily,
			String elementId, PeriodType periodType, Date instant,
			Date startDate, Date endDate) {
		ItemValue itemValue = null;
		if (PeriodType.INSTANT.equals(periodType)) {
			Date oneYearBeforeInstant = DateUtils.addYears(instant, -1);
			itemValue = itemFamily.getItemValue(elementId, periodType,
					oneYearBeforeInstant);
		} else if (PeriodType.DURATION.equals(periodType)) {
			Date oneYearBeforeStartDate = DateUtils.addYears(startDate, -1);
			Date oneYearBeforeEndDate = DateUtils.addYears(endDate, -1);
			itemValue = itemFamily.getItemValue(elementId, periodType,
					oneYearBeforeStartDate, oneYearBeforeEndDate);
		} else {
			throw new RuntimeException("PeriodType(" + periodType
					+ ") not implements !!!");
		}
		if (itemValue == null) {
			return null;
		}
		return itemValue.getValue();
	}

	private Set<Entry<ItemQualifier, ItemValue>> getLatestElementIdRecord(
			ItemFamily itemFamily) {
		String OldElementId = null;
		Set<Entry<HBaseColumnQualifier, HBaseValue>> qualValSet = itemFamily
				.getLatestQualifierAndValueAsDescendingSet();
		Map<ItemQualifier, ItemValue> map = new HashMap<ItemQualifier, ItemValue>(
				qualValSet.size());
		for (Entry<HBaseColumnQualifier, HBaseValue> qualValEnt : qualValSet) {
			ItemQualifier itemQual = (ItemQualifier) qualValEnt.getKey();
			String elementId = itemQual.getElementId();
			if (elementId.equals(OldElementId)) {
				continue;
			}
			ItemValue itemVal = (ItemValue) qualValEnt.getValue();
			map.put(itemQual, itemVal);
			OldElementId = elementId;
		}
		return map.entrySet();
	}

	private void generateGrowthFamily(Xbrl entity, Date ver) {
		ItemFamily itemFamily = entity.getItemFamily();
		GrowthFamily growthFamily = entity.getGrowthFamily();
		String OldElementId = null;
		for (Entry<ItemQualifier, ItemValue> qualValEnt : getLatestElementIdRecord(itemFamily)) {
			ItemQualifier itemQual = (ItemQualifier) qualValEnt.getKey();
			String elementId = itemQual.getElementId();
			if (elementId.equals(OldElementId)) {
				continue;
			}
			PeriodType periodType = itemQual.getPeriodType();
			Date instant = itemQual.getInstant();
			Date startDate = itemQual.getStartDate();
			Date endDate = itemQual.getEndDate();
			BigDecimal oneYearBeforeValue = getOneYearBeforeValue(itemFamily,
					elementId, periodType, instant, startDate, endDate);
			if (oneYearBeforeValue != null) {
				BigDecimal value = ((ItemValue) qualValEnt.getValue())
						.getValue();

				System.err
						.print(elementId + " / " + periodType + " / " + instant
								+ " / " + startDate + " / " + endDate + " / ");

				BigDecimal growthRatio = getGrowthRatio(value,
						oneYearBeforeValue);
				growthFamily.setRatio(elementId, periodType, instant,
						startDate, endDate, ver, growthRatio);
				BigDecimal growthRatioLn = getGrowthRatioNaturalLogarithm(growthRatio);
				growthFamily.setNaturalLogarithm(elementId, periodType,
						instant, startDate, endDate, ver, growthRatioLn);

				System.err.println(getGrowthRatio(value, oneYearBeforeValue)
						+ " / " + growthRatioLn);

			}
			OldElementId = elementId;
		}
	}

	private BigDecimal getGrowthRatio(BigDecimal value,
			BigDecimal oneYearBeforeValue) {
		return BigDecimalUtility.divide(value, oneYearBeforeValue);
	}

	private BigDecimal getGrowthRatioNaturalLogarithm(BigDecimal growthRatio) {
		if (growthRatio == null) {
			return null;
		}
		return BigDecimalUtility.getNaturalLogarithm(growthRatio);
	}

	private void generateItemFamily(Xbrl entity, Date ver) {
		ItemFamily itemFamily = entity.getItemFamily();
		for (Entry<HBaseColumnQualifier, NavigableMap<Date, HBaseValue>> qualEnt : entity
				.getInstanceFamily().getQualifierVersionValueSet()) {
			InstanceQualifier instQual = (InstanceQualifier) qualEnt.getKey();
			String elementId = instQual.getElementId();
			PeriodType periodType = instQual.getPeriodType();
			Date instant = instQual.getInstant();
			Date startDate = instQual.getStartDate();
			Date endDate = instQual.getEndDate();
			for (Entry<Date, HBaseValue> verEnt : qualEnt.getValue().entrySet()) {
				InstanceValue val = (InstanceValue) verEnt.getValue();
				BigDecimal value = getTwdValue(val.getUnitType(),
						val.getValue());
				itemFamily.setItemValue(elementId, periodType, instant,
						startDate, endDate, ver, value);

				// System.err.println(elementId + " / "
				// + periodType + " / " + instant
				// + " / " + startDate + " / "
				// + endDate + " / " + value);
			}
		}
	}

	private void generateInstanceFamily(Xbrl entity, ObjectNode objNode,
			Date ver) throws ParseException {
		InstanceFamily instanceFamily = entity.getInstanceFamily();
		JsonNode instanceNode = objNode.get(InstanceAssistant.INSTANCE);
		Iterator<Entry<String, JsonNode>> fields = instanceNode.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> eleIdEnt = fields.next();
			String eleId = eleIdEnt.getKey();
			ArrayNode arrNode = (ArrayNode) eleIdEnt.getValue();
			for (JsonNode dataNode : arrNode) {
				PeriodType periodType = PeriodType.getPeriodType(dataNode.get(
						"periodType").textValue());
				Date instant = getDate(dataNode.get("instant"));
				Date startDate = getDate(dataNode.get("startDate"));
				Date endDate = getDate(dataNode.get("endDate"));
				UnitType unitType = UnitType.getUnitType(dataNode.get("unit")
						.textValue());
				BigDecimal value = new BigDecimal(dataNode.get("value")
						.textValue());
				instanceFamily.setInstanceValue(eleId, periodType, instant,
						startDate, endDate, ver, unitType, value);
			}
		}
	}

	private void generateInfoFamilyVersion(Date ver, JsonNode infoNode,
			InfoFamily infoFamily) {
		String version = infoNode.get(InstanceAssistant.VERSION).textValue();
		infoFamily.setVersion(ver, XbrlTaxonomyVersion.valueOf(version));
	}

	private void generateInfoFamilyBalanceSheet(Date ver, JsonNode contextNode,
			InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode.get(Presentation.Id.BalanceSheet);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.INSTANT);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setBalanceSheetContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfComprehensiveIncome(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfComprehensiveIncome);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfComprehensiveIncomeContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfCashFlows(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfCashFlows);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfCashFlowsContext(ver, sb.toString());
	}

	private void generateInfoFamilyStatementOfChangesInEquity(Date ver,
			JsonNode contextNode, InfoFamily infoFamily) {
		StringBuilder sb = new StringBuilder();
		JsonNode presentIdNode = contextNode
				.get(Presentation.Id.StatementOfChangesInEquity);
		ArrayNode instantArrNode = (ArrayNode) presentIdNode
				.get(Instance.Attribute.DURATION);
		sb.setLength(0);
		for (JsonNode context : instantArrNode) {
			sb.append(context.textValue() + COMMA_STRING);
		}
		infoFamily.setStatementOfChangesInEquityContext(ver, sb.toString());
	}

	private void generateInfoFamily(Xbrl entity, ObjectNode objNode, Date ver) {
		InfoFamily infoFamily = entity.getInfoFamily();
		JsonNode infoNode = objNode.get(InstanceAssistant.INFO);
		generateInfoFamilyVersion(ver, infoNode, infoFamily);
		JsonNode contextNode = infoNode.get(InstanceAssistant.CONTEXT);
		generateInfoFamilyBalanceSheet(ver, contextNode, infoFamily);
		generateInfoFamilyStatementOfComprehensiveIncome(ver, contextNode,
				infoFamily);
		generateInfoFamilyStatementOfCashFlows(ver, contextNode, infoFamily);
		generateInfoFamilyStatementOfChangesInEquity(ver, contextNode,
				infoFamily);
	}

	private void generateRowKey(Xbrl entity, String stockCode,
			ReportType reportType, int year, int season) {
		entity.new RowKey(stockCode, reportType, year, season, entity);
	}
}

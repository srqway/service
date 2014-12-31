package idv.hsiehpinghan.botservice.entity;

import idv.hsiehpinghan.botservice.enumeration.Dollar;
import idv.hsiehpinghan.botservice.suit.TestngSuitSetting;

import java.io.IOException;
import java.util.Date;

import junit.framework.Assert;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ExchangeRateTest {

	@Test
	public void rowKey() {
		ExchangeRate rate = new ExchangeRate();
		ExchangeRate.Key key_1 = rate.new Key(Dollar.AUD, new Date());
		ExchangeRate.Key key_2 = rate.new Key();
		byte[] bs = key_1.toBytes();
		key_2.fromBytes(bs);
		Assert.assertEquals(key_1, key_2);
	}
}

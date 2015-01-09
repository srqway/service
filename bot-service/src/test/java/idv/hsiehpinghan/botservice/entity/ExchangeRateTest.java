package idv.hsiehpinghan.botservice.entity;

import idv.hsiehpinghan.botservice.enumeration.Dollar;

import java.util.Date;

import junit.framework.Assert;

import org.testng.annotations.Test;

public class ExchangeRateTest {

	@Test
	public void rowKey() {
		ExchangeRate rate = new ExchangeRate();
		ExchangeRate.Key key_1 = rate.new Key(Dollar.AUD, new Date(), rate);
		ExchangeRate.Key key_2 = rate.new Key(rate);
		byte[] bs = key_1.toBytes();
		key_2.fromBytes(bs);
		Assert.assertEquals(key_1, key_2);
	}
}

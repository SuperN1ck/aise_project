package nl.tudelft.serg.evosql.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountTest extends TestBase {
	
	@Test
	public void test1() {
		assertTrue(testExecutePath("SELECT COUNT(*) FROM PRODUCTS"));
	}
	
	@Test
	public void test2() {
		assertTrue(testExecutePath("SELECT COUNT(*) FROM PRODUCTS WHERE PRICE > 50"));
	}
	
	/**
	 * Test where the WHERE is always false but COUNT(*) still returns a value
	 *//*
	@Test
	public void test3() { // TODO Algorithm will attempt to solve the condition and will not reach distance 0, even though there is output.
		assertTrue(testExecutePath("SELECT COUNT(*) FROM Products WHERE Price > 50 AND Price < 50"));
	}*/
	
	@Test
	public void test4() {
		assertTrue(testExecutePath("SELECT COUNT(*) FROM PRODUCTS HAVING COUNT(*) > 0"));
	}
	
	@Test
	public void test5() {
		assertTrue(testExecutePath("SELECT COUNT(*) FROM PRODUCTS WHERE PRICE > 50 AND PRICE < 60 HAVING COUNT(*) > 0"));
	}

	/**
	 * Test for groups with at least 2 of the same products.
	 */
	@Test
	public void test6() {
		assertTrue(testExecutePath("SELECT PRODUCT, COUNT(*) FROM PRODUCTS GROUP BY PRODUCT HAVING COUNT(*) > 1"));
	}

	/**
	 * Test for groups with at least 4 of the same products.
	 */
	@Test
	public void test7() {
		assertTrue(testExecutePath("SELECT PRODUCT, COUNT(*) FROM PRODUCTS GROUP BY PRODUCT HAVING COUNT(*) > 3"));
	}

	/**
	 * Test for groups with at least some duplicate
	 */
	@Test
	public void test8() {
		assertTrue(testExecutePath("SELECT COUNT(PRODUCT) FROM PRODUCTS HAVING COUNT(PRODUCT) > COUNT(DISTINCT PRODUCT) AND COUNT(DISTINCT PRODUCT) > 1"));
	}

}

package dev.josafa.proxyserver.junit.tests;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

import dev.josafa.proxyserver.Utils;

class EnvironmentTests {

	@Test
	void test() throws NoSuchAlgorithmException, IOException {
		System.out.println(Utils.hasInCache("2e13e3aa33259f9bc0f19831e2d6ec65.cache"));
	}

}

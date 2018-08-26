/**
 * Copyright (c) 2017 - 2020, Wasiq Bhamla.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.wasiqb.coteafs.selenium.core;

import static com.github.wasiqb.coteafs.selenium.constants.ConfigKeys.BROWSER;
import static java.lang.System.getProperty;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import com.github.wasiqb.coteafs.selenium.config.ApplicationSetting;
import com.github.wasiqb.coteafs.selenium.config.AvailableBrowser;
import com.github.wasiqb.coteafs.selenium.config.ConfigUtil;
import com.github.wasiqb.coteafs.selenium.config.DelaySetting;
import com.github.wasiqb.coteafs.selenium.config.PlaybackSetting;

/**
 * @author Wasiq Bhamla
 * @since Aug 15, 2018 2:11:13 PM
 */
public class Browser {
	private static final ThreadLocal <EventFiringWebDriver>	driverThread;
	private static final Logger								log;
	private static ApplicationSetting						setting;

	static {
		driverThread = new ThreadLocal <> ();
		log = LogManager.getLogger (Browser.class);
		setting = ConfigUtil.appSetting ();
	}

	/**
	 * @author Wasiq Bhamla
	 * @since Aug 18, 2018 6:06:32 PM
	 */
	public static void close () {
		final int handles = driver ().getWindowHandles ()
				.size ();
		if (handles > 1) {
			driver ().close ();
		} else {
			stop ();
		}
	}

	/**
	 * @author Wasiq Bhamla
	 * @since Aug 15, 2018 2:14:24 PM
	 * @param browserName
	 */
	public static void start (final String browserName) {
		log.info ("Starting driver...");
		AvailableBrowser browser = null;
		if (browserName != null) {
			browser = AvailableBrowser.valueOf (browserName.toUpperCase ());
		} else {
			final String browserArg = getProperty (BROWSER, setting.getBrowser ()
					.name ());
			browser = AvailableBrowser.valueOf (browserArg.toUpperCase ());
		}
		final WebDriver driver = setupDriver (browser);
		final EventFiringWebDriver wd = new EventFiringWebDriver (driver);
		driver (wd);
		setupDriverOptions ();
	}

	/**
	 * @author Wasiq Bhamla
	 * @since Aug 15, 2018 2:37:22 PM
	 */
	public static void stop () {
		log.info ("Stopping driver...");
		driver ().quit ();
		driver (null);
	}

	static EventFiringWebDriver driver () {
		return Browser.driverThread.get ();
	}

	private static void driver (final EventFiringWebDriver driver) {
		driverThread.set (driver);
	}

	private static void manageOptions (final Consumer <Options> options) {
		options.accept (driver ().manage ());
	}

	private static void manageTimeouts (final Consumer <Timeouts> timeouts) {
		timeouts.accept (driver ().manage ()
				.timeouts ());
	}

	private static void manageWindow (final Consumer <Window> window) {
		window.accept (driver ().manage ()
				.window ());
	}

	/**
	 * @author Wasiq Bhamla
	 * @param playback
	 * @since Aug 19, 2018 5:03:31 PM
	 */
	private static void setScreenSize (final PlaybackSetting playback) {
		switch (playback.getScreenState ()) {
			case FULL_SCREEN:
				manageWindow (w -> w.fullscreen ());
				break;
			case MAXIMIZED:
				manageWindow (w -> w.maximize ());
				break;
			case NORMAL:
			default:
				manageWindow (w -> w.setSize (playback.getScreenResolution ()));
				break;

		}
	}

	private static WebDriver setupChromeDriver () {
		log.info ("Setting up Chrome driver...");
		final ChromeOptions chromeOptions = new ChromeOptions ();
		chromeOptions.addArguments ("--dns-prefetch-disable");
		chromeOptions.setCapability (CapabilityType.ACCEPT_SSL_CERTS, true);
		final ChromeDriverService chromeService = ChromeDriverService.createDefaultService ();
		return new ChromeDriver (chromeService, chromeOptions);
	}

	private static WebDriver setupDriver (final AvailableBrowser browser) {
		switch (browser) {
			case CHROME:
				return setupChromeDriver ();
			case FIREFOX:
			default:
				return setupFirefoxDriver ();
		}
	}

	private static void setupDriverOptions () {
		final PlaybackSetting playback = setting.getPlayback ();
		final DelaySetting delays = playback.getDelays ();
		manageTimeouts (t -> t.pageLoadTimeout (delays.getPageLoad (), TimeUnit.SECONDS));
		manageTimeouts (t -> t.setScriptTimeout (delays.getScriptLoad (), TimeUnit.SECONDS));
		manageTimeouts (t -> t.implicitlyWait (delays.getImplicit (), TimeUnit.SECONDS));
		manageOptions (Options::deleteAllCookies);
		setScreenSize (playback);
	}

	private static WebDriver setupFirefoxDriver () {
		log.info ("Setting up Firefox driver...");
		final DesiredCapabilities capabilities = DesiredCapabilities.firefox ();
		final FirefoxOptions options = new FirefoxOptions (capabilities);
		final GeckoDriverService firefoxService = GeckoDriverService.createDefaultService ();
		return new FirefoxDriver (firefoxService, options);
	}

	private Browser () {
		// This is a static util class.
	}
}
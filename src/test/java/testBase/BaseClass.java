
package testBase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Optional;

public class BaseClass {

    // Each TestNG test instance owns its browser session
    public WebDriver driver;

    // Logger instance
    public static final Logger logger =
            LogManager.getLogger(BaseClass.class);

    // Properties object
    public Properties prop;

    // =========================================================
    // Browser Setup and Application Initialization
    // =========================================================

    @BeforeClass(alwaysRun = true, groups = {
            "Master", "Sanity", "Regression", "DataDriven"
    })
    @Parameters({"browser", "os"})
    public void setup(String browser, @Optional("Windows") String os) {

        logger.info("=================================================");
        logger.info("          TEST SETUP STARTED");
        logger.info("=================================================");

        logger.info("BeforeClass setup() method execution started");
        logger.info("Initial WebDriver value: {}", driver);

        try {

            // -------------------------------------------------
            // Step 1: Load Configuration Properties
            // -------------------------------------------------

            prop = new Properties();

            try (FileReader file = new FileReader(
                    "src/test/resources/config.properties")) {

                prop.load(file);
            }

            logger.info("Configuration properties loaded successfully");

            // -------------------------------------------------
            // Step 2: Read Application URL
            // -------------------------------------------------

            String appURL = prop.getProperty("appURL");

            if (appURL == null || appURL.trim().isEmpty()) {
                throw new IllegalStateException(
                        "appURL is missing or empty in config.properties");
            }

            logger.info("Application URL: {}", appURL);

            // -------------------------------------------------
            // Step 3: Validate Browser Parameter
            // -------------------------------------------------

            if (browser == null || browser.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Browser parameter is null or empty");
            }

            logger.info("Selected browser: {}", browser);

            // -------------------------------------------------
            // Step 4: Initialize local or Grid browser session
            // -------------------------------------------------

            String executionEnv = prop.getProperty("execution_env", "local").trim();

            if (executionEnv.equalsIgnoreCase("remote")) {
                String gridUrl = prop.getProperty("grid_url", "").trim();
                if (gridUrl.isEmpty()) {
                    throw new IllegalStateException(
                            "grid_url is missing or empty in config.properties");
                }

                MutableCapabilities capabilities;
                if (browser.equalsIgnoreCase("chrome")) {
                    capabilities = new ChromeOptions();
                } else if (browser.equalsIgnoreCase("edge")) {
                    capabilities = new EdgeOptions();
                } else if (browser.equalsIgnoreCase("firefox")) {
                    capabilities = new FirefoxOptions();
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported browser: " + browser);
                }

                capabilities.setCapability("platformName", getGridPlatform(os));
                driver = new RemoteWebDriver(new URL(gridUrl), capabilities);
                logger.info("Remote WebDriver requested: browser={}, os={}, grid={}",
                        browser, os, gridUrl);

            } else if (executionEnv.equalsIgnoreCase("local")) {
                if (browser.equalsIgnoreCase("chrome")) {
                    driver = new ChromeDriver();
                } else if (browser.equalsIgnoreCase("edge")) {
                    driver = new EdgeDriver();
                } else if (browser.equalsIgnoreCase("firefox")) {
                    driver = new FirefoxDriver();
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported browser: " + browser);
                }
                logger.info("Local {} browser requested", browser);

            } else {
                throw new IllegalArgumentException(
                        "Unsupported execution_env: " + executionEnv
                                + ". Use local or remote.");
            }

            // Verify WebDriver initialization
            if (driver == null) {
                throw new IllegalStateException(
                        "WebDriver initialization failed");
            }

            logger.info("{} browser launched successfully", browser);
            logger.info("WebDriver initialized: {}", driver != null);

            // -------------------------------------------------
            // Step 5: Configure Browser Settings
            // -------------------------------------------------

            driver.manage().window().maximize();

            logger.info("Browser window maximized");

            driver.manage().deleteAllCookies();

            logger.info("All browser cookies deleted");

            driver.manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(10));

            logger.info("Implicit wait configured: 10 seconds");

            // -------------------------------------------------
            // Step 6: Navigate to Application
            // -------------------------------------------------

            logger.info("Navigating to application...");

            driver.get(appURL);

            logger.info("Application opened successfully");
            logger.info("Current URL: {}", driver.getCurrentUrl());

            logger.info("=================================================");
            logger.info("          TEST SETUP COMPLETED");
            logger.info("=================================================");

        } catch (Exception e) {

            logger.error("TEST SETUP FAILED", e);

            // Close browser if initialization was successful
            if (driver != null) {

                try {
                    driver.quit();
                    logger.info("Browser closed after setup failure");

                } catch (Exception closeException) {

                    logger.error(
                            "Failed to close browser after setup failure",
                            closeException
                    );

                } finally {
                    driver = null;
                }
            }

            // Fail the TestNG setup with original exception
            Assert.fail(
                    "Test setup failed: " + e.getMessage(),
                    e
            );
        }
    }

    /** Maps the TestNG OS parameter to a Grid platform capability. */
    private String getGridPlatform(String os) {
        if (os == null || os.trim().isEmpty()) {
            throw new IllegalArgumentException("OS parameter is null or empty");
        }

        String normalizedOs = os.trim().toLowerCase();
        if (normalizedOs.equals("windows") || normalizedOs.equals("win11")
                || normalizedOs.equals("windows 11")) {
            return Platform.WIN11.toString();
        }
        if (normalizedOs.equals("mac") || normalizedOs.equals("macos")) {
            return Platform.MAC.toString();
        }
        if (normalizedOs.equals("linux")) {
            return Platform.LINUX.toString();
        }
        throw new IllegalArgumentException("Unsupported OS parameter: " + os);
    }
    /** Captures a timestamped screenshot and returns its saved path. */
    public String captureScreen(String testName) throws IOException {
        if (driver == null) {
            throw new IllegalStateException("Cannot capture a screenshot without an active browser.");
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String safeTestName = testName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path screenshotDirectory = Paths.get("screenshots");
        Files.createDirectories(screenshotDirectory);

        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Path target = screenshotDirectory.resolve(safeTestName + "_" + timestamp + ".png");
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
    // =========================================================
    // Random Test Data Generation Utilities
    // =========================================================

    // Generate random alphabetic string
    public String randomString() {

        return RandomStringUtils.secure()
                .nextAlphabetic(8);
    }

    // Generate random numeric string
    public String randomNumber() {

        return RandomStringUtils.secure()
                .nextNumeric(10);
    }

    // =========================================================
    // Browser Teardown
    // =========================================================

    @AfterClass(alwaysRun = true)
    public void tearDown() {

        logger.info("=================================================");
        logger.info("          TEST TEARDOWN STARTED");
        logger.info("=================================================");

        if (driver != null) {

            try {

                driver.quit();

                logger.info("Browser closed successfully");

            } catch (Exception e) {

                logger.error("Error occurred while closing browser", e);

            } finally {

                driver = null;

                logger.info("WebDriver reference set to null");
            }

        } else {

            logger.warn(
                    "Browser session was not initialized; nothing to close"
            );
        }

        logger.info("=================================================");
        logger.info("          TEST TEARDOWN COMPLETED");
        logger.info("=================================================");
    }
}

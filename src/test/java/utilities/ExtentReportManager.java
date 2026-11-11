package utilities;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import testBase.BaseClass;

/** Creates one Extent report for each TestNG XML test context. */
public class ExtentReportManager implements ITestListener {

    private static final Logger LOGGER = LogManager.getLogger(ExtentReportManager.class);
    private static final String SESSION_ATTRIBUTE = "extentReportSession";
    private static final String CONFIG_FILE = "src/test/resources/config.properties";
    private static final String REPORT_DIRECTORY = "reports";

    @Override
    public void onStart(ITestContext context) {
        try {
            Path reportsPath = Paths.get(REPORT_DIRECTORY);
            Files.createDirectories(reportsPath);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String contextName = safeFileName(context.getName());
            Path reportPath = reportsPath.resolve(
                    "TestNGReport_" + contextName + "_" + timestamp + ".html");

            ExtentSparkReporter sparkReporter =
                    new ExtentSparkReporter(reportPath.toString());
            sparkReporter.config().setDocumentTitle("OpenCart Automation Report");
            sparkReporter.config().setReportName("OpenCart Test Execution Results");
            sparkReporter.config().setTheme(Theme.DARK);

            ExtentReports extent = new ExtentReports();
            extent.attachReporter(sparkReporter);
            addSystemInformation(extent, context);

            context.setAttribute(SESSION_ATTRIBUTE,
                    new ReportSession(extent, reportPath.toAbsolutePath()));
            LOGGER.info("Extent report started: {}", reportPath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Could not create the Extent report for {}", context.getName(), e);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = createTest(result);
        if (test != null) {
            test.log(Status.PASS, result.getName() + " passed.");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = createTest(result);
        if (test == null) {
            return;
        }

        test.log(Status.FAIL, result.getThrowable());
        attachFailureScreenshot(test, result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = createTest(result);
        if (test != null) {
            if (result.getThrowable() == null) {
                test.log(Status.SKIP, result.getName() + " was skipped.");
            } else {
                test.log(Status.SKIP, result.getThrowable());
            }
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        ReportSession session = getSession(context);
        if (session == null) {
            return;
        }

        session.extent.flush();
        LOGGER.info("Extent report saved: {}", session.reportPath);

        Properties properties = loadProperties();
        if (Boolean.parseBoolean(properties.getProperty("report.email.enabled", "false"))) {
            sendReportByEmail(session.reportPath, properties);
        }

        if (Boolean.parseBoolean(properties.getProperty("report.autoOpen", "true"))) {
            openReport(session.reportPath);
        }
    }

    private void addSystemInformation(ExtentReports extent, ITestContext context) {
        extent.setSystemInfo("Application", "OpenCart");
        extent.setSystemInfo("Module", "Customer Account");
        extent.setSystemInfo("Sub Module", "Registration and Login");
        extent.setSystemInfo("Tester", System.getProperty("user.name", "unknown"));
        extent.setSystemInfo("Environment", "QA");

        String operatingSystem = context.getCurrentXmlTest().getParameter("os");
        String browser = context.getCurrentXmlTest().getParameter("browser");
        extent.setSystemInfo("Operating System", displayOrUnknown(operatingSystem));
        extent.setSystemInfo("Browser", displayOrUnknown(browser));

        List<String> includedGroups = context.getCurrentXmlTest().getIncludedGroups();
        if (includedGroups.isEmpty()) {
            XmlSuite suite = context.getSuite().getXmlSuite();
            includedGroups = suite.getIncludedGroups();
        }
        if (!includedGroups.isEmpty()) {
            extent.setSystemInfo("Included Groups", String.join(", ", includedGroups));
        }
    }

    private ExtentTest createTest(ITestResult result) {
        ReportSession session = getSession(result.getTestContext());
        if (session == null) {
            LOGGER.warn("No Extent report session exists for {}", result.getName());
            return null;
        }

        String testCaseName = result.getTestClass().getRealClass().getSimpleName();
        ExtentTest test = session.extent.createTest(testCaseName);
        String[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            test.assignCategory(groups);
        }
        return test;
    }

    private void attachFailureScreenshot(ExtentTest test, ITestResult result) {
        Object testInstance = result.getInstance();
        if (!(testInstance instanceof BaseClass)) {
            LOGGER.warn("Cannot capture a screenshot for {}: test does not extend BaseClass",
                    result.getName());
            return;
        }

        try {
            String screenshot = ((BaseClass) testInstance).captureScreen(result.getName());
            Path reportDirectory = Paths.get(REPORT_DIRECTORY).toAbsolutePath().normalize();
            Path screenshotPath = Paths.get(screenshot).toAbsolutePath().normalize();
            String reportRelativePath = reportDirectory.relativize(screenshotPath)
                    .toString().replace(File.separatorChar, '/');
            test.addScreenCaptureFromPath(reportRelativePath, "Failure screenshot");
        } catch (Exception e) {
            LOGGER.warn("Could not attach a screenshot for {}", result.getName(), e);
        }
    }

    private void openReport(Path reportPath) {
        if (!Desktop.isDesktopSupported()) {
            LOGGER.info("Desktop browser launch is not available; open the report manually.");
            return;
        }

        try {
            Desktop.getDesktop().browse(reportPath.toUri());
        } catch (IOException e) {
            LOGGER.warn("Could not open the Extent report automatically: {}", reportPath, e);
        }
    }

    /** Sends the report only when report.email.enabled is explicitly set to true. */
    private void sendReportByEmail(Path reportPath, Properties properties) {
        try {
            String host = requiredProperty(properties, "report.email.smtp.host");
            int port = Integer.parseInt(requiredProperty(properties, "report.email.smtp.port"));
            String username = requiredProperty(properties, "report.email.username");
            String password = requiredProperty(properties, "report.email.password");
            String from = requiredProperty(properties, "report.email.from");
            String to = requiredProperty(properties, "report.email.to");

            HtmlEmail email = new HtmlEmail();
            email.setHostName(host);
            email.setSmtpPort(port);
            email.setAuthenticator(new DefaultAuthenticator(username, password));
            email.setSSLOnConnect(Boolean.parseBoolean(
                    properties.getProperty("report.email.ssl", "true")));
            email.setFrom(from);
            email.addTo(to);
            email.setSubject(properties.getProperty(
                    "report.email.subject", "OpenCart automation report"));
            email.setHtmlMsg("<html><body><p>Automation execution is complete.</p>"
                    + "<p>The attached file contains the test report.</p></body></html>");
            email.setTextMsg("Automation execution is complete. See the attached report.");
            email.attach(reportPath.toFile());
            email.send();
            LOGGER.info("Automation report emailed to {}", to);
        } catch (Exception e) {
            LOGGER.error("Could not send the automation report email", e);
        }
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            properties.load(reader);
        } catch (IOException e) {
            LOGGER.warn("Could not load report settings from {}", CONFIG_FILE, e);
        }
        return properties;
    }

    private String requiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Set " + key + " before enabling report email.");
        }
        return value;
    }

    private String displayOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "Not specified" : value;
    }

    private String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private ReportSession getSession(ITestContext context) {
        Object session = context.getAttribute(SESSION_ATTRIBUTE);
        return session instanceof ReportSession ? (ReportSession) session : null;
    }

    private static class ReportSession {
        private final ExtentReports extent;
        private final Path reportPath;

        private ReportSession(ExtentReports extent, Path reportPath) {
            this.extent = extent;
            this.reportPath = reportPath;
        }
    }
}

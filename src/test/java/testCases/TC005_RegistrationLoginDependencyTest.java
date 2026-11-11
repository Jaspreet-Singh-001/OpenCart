package testCases;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.homePage;
import pageObjects.loginPage;
import pageObjects.myAccountPage;
import testBase.BaseClass;
import utilities.ExcelUtility;

public class TC005_RegistrationLoginDependencyTest extends BaseClass {

    private static final String DATA_FILE = ".\\testData\\OpenCartLoginData.xlsx";
    private static final String REGISTRATION_SHEET = "Sheet3";
    private static final int GENERATED_USER_COUNT = 5;
    private static final By MY_ACCOUNT_TITLE =
            By.xpath("//h2[normalize-space()='My Account']");
    private static final By LOGIN_ERROR =
            By.cssSelector(".alert.alert-danger");

    @Test(
        dependsOnGroups = "GeneratedRegistration",
        groups = {"Regression", "Master", "DataDriven"}
    )
    public void verifyLatestGeneratedRegistrationsCanLogin() throws IOException {
        List<String[]> accounts = readLatestGeneratedAccounts();
        List<String> failures = new ArrayList<>();

        // Use explicit waits for the login result and avoid mixing wait strategies.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            for (int i = 0; i < accounts.size(); i++) {
                String email = accounts.get(i)[0];
                String password = accounts.get(i)[1];

                try {
                    // Reset the browser session between data rows.
                    driver.manage().deleteAllCookies();
                    driver.get(prop.getProperty("appURL"));

                    homePage home = new homePage(driver);
                    home.clickMyAccountButton();
                    home.clickLoginButton();

                    loginPage login = new loginPage(driver);
                    login.enterEmail(email);
                    login.enterPassword(password);
                    login.clickLoginButton();

                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.visibilityOfElementLocated(MY_ACCOUNT_TITLE),
                            ExpectedConditions.visibilityOfElementLocated(LOGIN_ERROR)
                    ));

                    boolean accountPageVisible = driver.findElements(MY_ACCOUNT_TITLE).stream()
                            .anyMatch(WebElement::isDisplayed);
                    if (!accountPageVisible) {
                        failures.add("record " + (i + 1));
                        logger.error("Login failed for generated account record {}", i + 1);
                        continue;
                    }

                    logger.info("Login succeeded for generated account record {}", i + 1);
                    new myAccountPage(driver).clickLogout();
                } catch (Exception e) {
                    failures.add("record " + (i + 1));
                    logger.error("Login validation failed for generated account record {}",
                            i + 1, e);
                }
            }
        } finally {
            // Restore the wait configured by BaseClass for other test classes.
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        Assert.assertTrue(
                failures.isEmpty(),
                "Login failed for generated account(s): " + String.join(", ", failures)
        );
    }

    private List<String[]> readLatestGeneratedAccounts() throws IOException {
        ExcelUtility excel = new ExcelUtility(DATA_FILE);
        int lastRow = excel.getRowCount(REGISTRATION_SHEET);

        Assert.assertTrue(
                lastRow >= GENERATED_USER_COUNT,
                "Sheet3 does not contain five generated registration rows."
        );

        int firstRow = lastRow - GENERATED_USER_COUNT + 1;
        List<String[]> accounts = new ArrayList<>();

        for (int row = firstRow; row <= lastRow; row++) {
            String email = excel.getCellData(REGISTRATION_SHEET, row, 0).trim();
            String password = excel.getCellData(REGISTRATION_SHEET, row, 1).trim();
            String expected = excel.getCellData(REGISTRATION_SHEET, row, 2).trim();

            Assert.assertFalse(email.isEmpty(),
                    "Missing email in Sheet3 row " + (row + 1));
            Assert.assertFalse(password.isEmpty(),
                    "Missing password in Sheet3 row " + (row + 1));
            Assert.assertEquals(expected, "Valid",
                    "Unexpected registration status in Sheet3 row " + (row + 1));

            accounts.add(new String[] {email, password});
        }

        logger.info("Loaded the latest {} generated accounts from Sheet3",
                accounts.size());
        return accounts;
    }
}
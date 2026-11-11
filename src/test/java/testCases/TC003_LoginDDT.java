package testCases;

import java.time.Duration;

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
import utilities.DataProviders;

public class TC003_LoginDDT extends BaseClass {

    private static final By MY_ACCOUNT_TITLE =
            By.xpath("//h2[normalize-space()='My Account']");
    private static final By LOGIN_ERROR =
            By.cssSelector(".alert.alert-danger");
    private static final By MY_ACCOUNT_MENU =
            By.xpath("//a[@title='My Account']");
    private static final By LOGIN_MENU_LINK =
            By.xpath("//a[normalize-space()='Login']");
    private static final By EMAIL_FIELD = By.id("input-email");
    private static final By PASSWORD_FIELD = By.id("input-password");
    private static final By LOGIN_BUTTON =
            By.xpath("//input[@value='Login']");
    private static final By LOGOUT_LINK =
            By.xpath("(//a[text()='Logout'])[2]");
    private static final Duration ELEMENT_TIMEOUT = Duration.ofSeconds(15);

    @Test(
        dataProvider = "LoginData",
        dataProviderClass = DataProviders.class,
        groups = {"Regression", "Master", "DataDriven"}
    )
    public void verify_loginDDT(String email, String password, String expected) {
        Assert.assertNotNull(email, "Email from LoginData must not be null");
        Assert.assertNotNull(password, "Password from LoginData must not be null");
        Assert.assertNotNull(expected, "Expected result from LoginData must not be null");

        String expectedResult = expected.trim();
        logger.info("Checking login data marked as {}", expectedResult);
        Assert.assertTrue(
                expectedResult.equalsIgnoreCase("Valid")
                        || expectedResult.equalsIgnoreCase("Invalid"),
                "Unsupported expected result in LoginData: " + expected
        );

        // Use explicit waits because the account menu and login form load dynamically.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        try {
            driver.get(prop.getProperty("appURL"));

            homePage home = new homePage(driver);
            wait.until(ExpectedConditions.elementToBeClickable(MY_ACCOUNT_MENU));
            home.clickMyAccountButton();
            wait.until(ExpectedConditions.elementToBeClickable(LOGIN_MENU_LINK));
            home.clickLoginButton();

            loginPage login = new loginPage(driver);
            wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
            login.enterEmail(email);
            wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
            login.enterPassword(password);
            wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
            login.clickLoginButton();

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(MY_ACCOUNT_TITLE),
                    ExpectedConditions.visibilityOfElementLocated(LOGIN_ERROR)
            ));

            boolean validCredentials = driver.findElements(MY_ACCOUNT_TITLE).stream()
                    .anyMatch(WebElement::isDisplayed);
            boolean loginRejected = driver.findElements(LOGIN_ERROR).stream()
                    .anyMatch(WebElement::isDisplayed);

            if (expectedResult.equalsIgnoreCase("Valid")) {
                Assert.assertTrue(
                        validCredentials,
                        "Expected a successful login for the supplied test data."
                );

                logger.info("Login succeeded for this data row");
                // Leave the shared browser logged out before the next data row.
                wait.until(ExpectedConditions.elementToBeClickable(LOGOUT_LINK));
                new myAccountPage(driver).clickLogout();
            } else {
                Assert.assertTrue(
                        loginRejected && !validCredentials,
                        "Expected invalid credentials to keep the user on the login page "
                                + "and display the login error."
                );
            }
        } finally {
            // Restore the wait configured by BaseClass for the remaining test classes.
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }
}
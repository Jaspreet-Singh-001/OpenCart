package testCases;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.homePage;
import pageObjects.loginPage;
import pageObjects.myAccountPage;
import testBase.BaseClass;

public class TC004_SimpleLoginTest extends BaseClass {

    @Test(groups = {"Sanity", "Master"})
    public void verify_login() {
        logger.info("Starting simple login test");

        homePage home = new homePage(driver);
        home.clickMyAccountButton();
        home.clickLoginButton();

        String email = prop.getProperty("email");
        String password = prop.getProperty("password");

        loginPage login = new loginPage(driver);
        login.enterEmail(email);
        login.enterPassword(password);
        login.clickLoginButton();

        myAccountPage account = new myAccountPage(driver);
        Assert.assertTrue(
                account.isMyAccountTitleDisplayed(),
                "Login failed! My Account page was not displayed."
        );
        logger.info("Login succeeded");

        account.clickLogout();
        logger.info("Simple login test completed");
    }
}

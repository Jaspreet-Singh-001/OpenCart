package testCases;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.homePage;
import pageObjects.myAccountPage;
import pageObjects.registerPage;
import testBase.BaseClass;
import utilities.ExcelUtility;

public class TC002_OpenCartRegistrationDDT extends BaseClass {

    private static final String DATA_FILE = ".\\testData\\OpenCartLoginData.xlsx";
    private static final String REGISTRATION_SHEET = "Sheet3";
    private static final String REGISTRATION_SUCCESS_MESSAGE =
            "Your Account Has Been Created!";

    @Test(invocationCount = 5, groups = {"DataDriven", "Master", "GeneratedRegistration"})
    public void registrationDDT() {
        logger.info("Starting Registration DDT test");

        try {
            homePage home = new homePage(driver);
            registerPage registration = new registerPage(driver);

            home.clickMyAccountButton();
            home.clickRegisterButton();

            String firstName = randomString().toUpperCase();
            String lastName = randomString().toUpperCase();
            String email = randomString().toLowerCase()
                    + System.currentTimeMillis() + "@gmail.com";
            String telephone = randomNumber();
            String password = randomString();

            logger.info("Submitting registration for {}", email);
            registration.setFirstName(firstName);
            registration.setLastName(lastName);
            registration.setEmail(email);
            registration.setTelephoneNumber(telephone);
            registration.setPassword(password);
            registration.setConfirmPassword(password);
            registration.selectNewsletterYes();
            registration.acceptPrivacyPolicy();
            registration.submitRegistration();

            String actualMessage = registration.getConfirmationMessage();
            Assert.assertEquals(
                    actualMessage,
                    REGISTRATION_SUCCESS_MESSAGE,
                    "Registration failed for: " + email
            );
            logger.info("Registration succeeded for {}", email);

            new myAccountPage(driver).clickLogout();

            ExcelUtility excel = new ExcelUtility(DATA_FILE);
            int newRow = excel.getRowCount(REGISTRATION_SHEET) + 1;
            excel.setRowData(REGISTRATION_SHEET, newRow, email, password, "Valid");

            logger.info("Saved registration credentials to {} row {}",
                    REGISTRATION_SHEET, newRow + 1);
        } catch (Exception e) {
            logger.error("Registration DDT failed", e);
            Assert.fail("Registration DDT failed: " + e.getMessage(), e);
        }
    }
}

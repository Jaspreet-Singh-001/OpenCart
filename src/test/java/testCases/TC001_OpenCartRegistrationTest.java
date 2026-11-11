
package testCases;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.homePage;
import pageObjects.registerPage;
import testBase.BaseClass;

public class TC001_OpenCartRegistrationTest extends BaseClass {

    @Test(groups = {"Sanity","Master"})
    public void registrationTest() {

        logger.info("===== Starting OpenCart Registration Test =====");

        try {
            // Initialize Page Object classes
            homePage hp = new homePage(driver);
            registerPage rp = new registerPage(driver);

            // Navigate to the Registration page
            logger.info("Clicking on My Account");
            hp.clickMyAccountButton();

            logger.info("Clicking on Register");
            hp.clickRegisterButton();

            // Generate random user details
            String firstName = randomString().toUpperCase();
            String lastName = randomString().toUpperCase();
            String email = randomString() + "@gmail.com";
            String telephone = randomNumber();
            String password = randomString();

            // Enter registration details
            logger.info("Entering registration details");
            rp.setFirstName(firstName);
            rp.setLastName(lastName);
            rp.setEmail(email);
            rp.setTelephoneNumber(telephone);
            rp.setPassword(password);
            rp.setConfirmPassword(password);

            // Complete registration form
            logger.info("Selecting newsletter option");
            rp.selectNewsletterYes();

            logger.info("Accepting Privacy Policy");
            rp.acceptPrivacyPolicy();

            logger.info("Submitting registration form");
            rp.submitRegistration();

            // Verify successful registration
            String actualMessage = rp.getConfirmationMessage();
            String expectedMessage = "Your Account Has Been Created!";

            logger.info("Verifying registration confirmation message");
            Assert.assertEquals(
                    actualMessage,
                    expectedMessage,
                    "Registration confirmation message did not match!"
            );

            logger.info("Registration test passed successfully");

        } catch (Exception e) {
            // Log the exception and fail the test with the original cause
            logger.error("Registration test failed due to an exception", e);
            Assert.fail("Registration test failed: " + e.getMessage(), e);

        } finally {
            logger.info("===== OpenCart Registration Test Execution Completed =====");
        }
    }
}

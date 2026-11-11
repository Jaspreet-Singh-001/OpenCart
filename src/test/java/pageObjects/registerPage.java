package pageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class registerPage extends base {

    @FindBy(id = "input-firstname")
    private WebElement firstNameField;

    @FindBy(id = "input-lastname")
    private WebElement lastNameField;

    @FindBy(id = "input-email")
    private WebElement emailField;

    @FindBy(id = "input-telephone")
    private WebElement telephoneField;

    @FindBy(id = "input-password")
    private WebElement passwordField;

    @FindBy(id = "input-confirm")
    private WebElement confirmPasswordField;

    @FindBy(xpath = "//label[normalize-space()='Yes']")
    private WebElement newsletterYesOption;

    @FindBy(name = "agree")
    private WebElement privacyPolicyCheckbox;

    @FindBy(xpath = "//input[@value='Continue']")
    private WebElement continueButton;

    @FindBy(xpath = "//h1[normalize-space()='Your Account Has Been Created!']")
    private WebElement confirmationMessage;

    public registerPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public void setFirstName(String firstName) {
        firstNameField.sendKeys(firstName);
    }

    public void setLastName(String lastName) {
        lastNameField.sendKeys(lastName);
    }

    public void setEmail(String email) {
        emailField.sendKeys(email);
    }

    public void setTelephoneNumber(String telephoneNumber) {
        telephoneField.sendKeys(telephoneNumber);
    }

    public void setPassword(String password) {
        passwordField.sendKeys(password);
    }

    public void setConfirmPassword(String password) {
        confirmPasswordField.sendKeys(password);
    }

    public void selectNewsletterYes() {
        newsletterYesOption.click();
    }

    public void acceptPrivacyPolicy() {
        privacyPolicyCheckbox.click();
    }

    public void submitRegistration() {
        continueButton.click();
    }

    public String getConfirmationMessage() {
        return confirmationMessage.getText();
    }
}

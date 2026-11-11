package pageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class homePage extends base {

    @FindBy(xpath = "//a[@title='My Account']")
    private WebElement myAccountButton;

    @FindBy(xpath = "//a[normalize-space()='Register']")
    private WebElement registerButton;

    @FindBy(xpath = "//a[normalize-space()='Login']")
    private WebElement loginButton;

    public homePage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public void clickMyAccountButton() {
        myAccountButton.click();
    }

    public void clickRegisterButton() {
        registerButton.click();
    }

    public void clickLoginButton() {
        loginButton.click();
    }
}

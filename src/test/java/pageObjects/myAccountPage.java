package pageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class myAccountPage extends base {

    @FindBy(xpath = "//h2[normalize-space()='My Account']")
    private WebElement myAccountTitle;

    @FindBy(xpath = "(//a[text()='Logout'])[2]")
    private WebElement logoutButton;

    public myAccountPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isMyAccountTitleDisplayed() {
        return myAccountTitle.isDisplayed();
    }

    public void clickLogout() {
        logoutButton.click();
    }
}

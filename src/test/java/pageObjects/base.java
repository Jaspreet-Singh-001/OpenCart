package pageObjects;

import org.openqa.selenium.WebDriver;

public class base {

    protected final WebDriver driver;

    public base(WebDriver driver) {
        this.driver = driver;
    }
}

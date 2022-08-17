package driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class DriverFactory {
    public static WebDriver getDriver() {
        String browser = System.getenv("BROWSER");
        browser = (browser == null) ? "CHROME" : browser;

        switch (browser) {
            case "IE":
                WebDriverManager.iedriver().setup();
                return new InternetExplorerDriver();
            case "FIREFOX":
                WebDriverManager.firefoxdriver().setup();
                return new FirefoxDriver();
            case "CHROME":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                options.setExperimentalOption("excludeSwitches", Arrays.asList("disable-popup-blocking"));

                HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
                chromePrefs.put("profile.default_content_settings.popups", 2);
                chromePrefs.put("profile.default_content_settings.javascript", 1);
                chromePrefs.put("credentials_enable_service", false);
                chromePrefs.put("download.prompt_for_download", false);
                chromePrefs.put("profile.content_settings.exceptions.automatic_downloads.*.setting", 1);
                chromePrefs.put("safebrowsing.enabled", true);
                chromePrefs.put("network.cookie.cookieBehavior", 2);


                options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
                //options.setExperimentalOption("useAutomationExtension", false);
                options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
                options.setExperimentalOption("detach", false);
                options.setExperimentalOption("prefs", chromePrefs);

                options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                //options.setCapability("enableVNC", true);
                //options.setCapability("version", "67.0");

                options.addArguments("disable-popup-blocking");
                options.addArguments("--disable-infobars");
                options.addArguments("--test-type");
                options.addArguments("--disable-extensions");
                options.addArguments("--ignore-certificate-errors");
                options.addArguments("--enable-javascript");
                options.addArguments("--start-maximized");
                options.merge(options);

                System.setProperty("webdriver.chrome.driver", (String) System.getenv().getOrDefault("webdriver-chrome-driver", "web_driver/chromedriver.exe"));

                return new ChromeDriver(options);
        }
    }

}

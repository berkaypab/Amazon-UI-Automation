package driver;

import WebAutomationBase.model.ElementInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.gauge.*;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Driver {
    protected static SoftAssertions softAssertions = new SoftAssertions();
    public static ConcurrentMap<String, Object> hashMapList = new ConcurrentHashMap();
    protected static Actions action;
    public static ConcurrentMap<String, Object> elementMapList = new ConcurrentHashMap();
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DEFAULT_DIRECTORY_PATH = "elementValues";
    protected static WebDriver driver;

    public Driver() {


    }

    @BeforeScenario
    public void setUp() {
        driver = DriverFactory.getDriver();
        action = new Actions(driver);
    }

    public void initMap(File[] fileList) {
        Type elementType = (new TypeToken<List<ElementInfo>>() {
        }).getType();
        Gson gson = new Gson();
        List<ElementInfo> elementInfoList;


        for (File file : fileList) {
            try {
                elementInfoList = gson.fromJson(new FileReader(file), elementType);
                elementInfoList.parallelStream().forEach((elementInfo) -> {
                    elementMapList.put(elementInfo.getKey(), elementInfo);
                });
            } catch (FileNotFoundException var10) {
                this.logger.warn("{} not found", var10);
            }
        }
    }

    public File[] getFileList() {
        File[] fileList = new File(
                this.getClass().getClassLoader().getResource(DEFAULT_DIRECTORY_PATH).getFile())
                .listFiles(pathname -> !pathname.isDirectory() && pathname.getName().endsWith(".json"));
        if (fileList == null) {
            this.logger.warn("File Directory Is Not Found! Please Check Directory Location. Default Directory Path = {}", DEFAULT_DIRECTORY_PATH);
            throw new NullPointerException();
        } else {
            return fileList;
        }
    }

    public ElementInfo findElementInfoByKey(String key) {
        ElementInfo elementInfo = null;
        try {
            elementInfo = (ElementInfo) elementMapList.get(key);
        } catch (Exception e) {
        }
        if (elementInfo == null) {
            new Exception("Belirtilen key: '" + key + "' degeri " + DEFAULT_DIRECTORY_PATH + " dosyasi altindaki jsonlarda bulunamadi.");
        }
        return elementInfo;
    }

    public void saveValue(String key, String value) {
        elementMapList.put(key, value);
        this.logger.info("'" + value + "' degeri '" + key + "' keyi olarak saklandi.");
    }

    public String getValue(String key) {
        return elementMapList.get(key).toString();
    }

    public void setElementToJson(String key, String value, String type) {
        ElementInfo elementInfo = new ElementInfo();
        elementInfo.setKey(key);
        elementInfo.setValue(value);
        elementInfo.setType(type);
        elementMapList.put(key, elementInfo);
    }

    public void setHashMapValue(String hashMapKey, Object hashMap) {
        hashMapList.put(hashMapKey, hashMap);
    }

    public Object getHashMapValue(String hashMapKey) {
        return hashMapList.get(hashMapKey);
    }

    public WebDriver getWebDriver() {
        return driver;
    }

    @AfterStep
    public void screenshot() {
        try {
            Thread.sleep(10);
            takesScreenshot(String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takesScreenshot(String pictureName) {
        try {
            String fileName = "screenshot-images/" + pictureName + ".png";
            File file = new File("reports/html-report/" + fileName);
            if (file.exists()) {
                file.delete();
            }
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(scrFile, file);
            Gauge.writeMessage("<img src='../" + fileName + "' width='800' height='480'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterScenario
    public void closeDriverAfterScenario() {
        softAssertions.assertAll();
        driver.quit();

    }



}

package org.example;

import WebAutomationBase.model.ElementInfo;
import com.thoughtworks.gauge.Step;
import driver.Driver;
import org.junit.Assert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.*;

public class StepImplementation extends Driver {
    public static int DEFAULT_MAX_ITERATION_COUNT = 150;
    public static int DEFAULT_MILLISECOND_WAIT_AMOUNT = 100;

    public StepImplementation() {
        this.initMap(this.getFileList());
    }

    public By getElementInfoToBy(ElementInfo elementInfo) {
        By by = null;
        switch (elementInfo.getType()) {
            case "css":
                by = By.cssSelector(elementInfo.getValue());
                break;
            case "name":
                by = By.name(elementInfo.getValue());
                break;
            case "id":
                by = By.id(elementInfo.getValue());
                break;
            case "xpath":
                by = By.xpath(elementInfo.getValue());
                break;
            case "linkText":
                by = By.linkText(elementInfo.getValue());
                break;
            case "partialLinkText":
                by = By.partialLinkText(elementInfo.getValue());
                break;
        }
        return by;
    }


    private WebElement findElement(String key) {
        By infoParam = this.getElementInfoToBy(this.findElementInfoByKey(key));
        WebDriverWait webDriverWait = new WebDriverWait(driver, 20);
        WebElement webElement = webDriverWait
                .until(ExpectedConditions.presenceOfElementLocated(infoParam));
        webDriverWait.until((ExpectedCondition<Boolean>) wd ->
                ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'})",
                webElement);
        highlightElement(webElement);
        return webElement;
    }

    private List<WebElement> findElements(String key) {
        return driver.findElements(this.getElementInfoToBy(this.findElementInfoByKey(key)));
    }

    protected void waitForPageLoad(int timeout) {
        logger.debug("Waiting for page to load, timeout is: " + timeout);
        boolean pageLoadExceptionOccurred = false;
        FluentWait wait = (new FluentWait(driver)).withTimeout(Duration.ofSeconds((long) timeout)).pollingEvery(Duration.ofMillis(250L));

        try {
            wait.until(new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    boolean retVal = false;
                    String windowAlertMsg = "";

                    try {
                        windowAlertMsg = driver.switchTo().alert().getText();
                        logger.warn("Alert message has been displayed: " + windowAlertMsg);
                        retVal = true;
                        return retVal;
                    } catch (Exception var7) {
                        try {
                            String pageTitle = driver.getTitle();
                            logger.debug("Page title is: " + pageTitle);
                            retVal = ((JavascriptExecutor) driver).executeScript("return document.readyState", new Object[0]).toString().equalsIgnoreCase("complete");
                        } catch (TimeoutException var5) {
                            logger.warn("TimeoutException during waitForPageToLoad: " + var5.getMessage());
                        } catch (Exception var6) {
                            logger.warn("Exception during waitForPageToLoad: " + var6.getMessage());
                        }

                        return retVal;
                    }
                }
            });
        } catch (Throwable var10) {
            String errMsgTxt = "Timeout (" + timeout + " secs) waiting for Page Load Request to complete. About to click 'Stop' button of the browser.\nException: " + var10.getMessage();
            logger.warn(errMsgTxt);
            pageLoadExceptionOccurred = true;

            try {
                logger.debug("About to set page load timeout to: " + 20);
                driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
            } catch (Exception var9) {
                logger.warn("Could not set page load timeout to: " + 20);
                var9.printStackTrace();
            }

            try {
                logger.debug("About to set script timeout to: " + 20);
                driver.manage().timeouts().setScriptTimeout(20, TimeUnit.SECONDS);
            } catch (Exception var8) {
                logger.warn("Could not set script timeout to: " + 20);
                var8.printStackTrace();
            }

            try {
                logger.warn("About to stop browser navigation");
                ((JavascriptExecutor) driver).executeScript("window.stop();", new Object[0]);
            } catch (Exception var7) {
            }
        }

        logger.debug("Page loaded");
    }

    public void highlightElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("var element = arguments[0]; element.style.backgroundColor='#FF7960'; element.style.border='3px solid #FF7960';", new Object[]{element});
        } catch (Exception var3) {
        }
    }

    private void clickElement(WebElement element) {
        element.click();
    }

    private void clickElementBy(String key) {
        findElement(key).click();
    }

    private void sendKeys(String text, String key) {
        if (!key.equals("")) {
            findElement(key).sendKeys(text);
            logger.info(key + " elementine " + text + " texti yazildi.");
        }
    }

    private void clearTextArea(String key) {
        findElement(key).clear();
        logger.info("Text area cleared ");
    }

    private String getText(String key) {
        logger.info("Getting text from given locator ");
        return findElement(key).getText();
    }
    @Step("Navigate Back")
    private void navigateBack() {
        driver.navigate().back();
    }

   @Step("Refresh Page")
    public void refreshPage() {
        driver.navigate().refresh();
    }

    private boolean stockControl() {
        List<WebElement> list = findElements("productAvailabilitySpan");
        boolean exist = list.size() != 0;
        if (exist) {
            return true;
        } else {
            return false;
        }
    }

    @Step("<productsKey> ile rastgele ürün secilir")
    public void chooseRandomProductUntilInStock(String productsKey) {
        while (!stockControl()) {
            clickElement(randomPick(productsKey));
            if (stockControl()) {
                logger.info("stokta var");
            } else {
                logger.info("stokta yok");
                navigateBack();
            }
        }
    }

    private String getTitle() {
        return driver.getTitle();
    }

    private JavascriptExecutor getJSExecutor() {
        return (JavascriptExecutor) driver;
    }

    private Object executeJS(String script, boolean wait) {
        return wait ? getJSExecutor().executeScript(script, "") : getJSExecutor().executeAsyncScript(script, "");
    }

    private void scrollTo(int x, int y) {
        String script = String.format("window.scrollTo(%d, %d);", x, y);
        executeJS(script, true);
    }

    private WebElement scrollToElementToBeVisible(String key) {
        WebElement e = findElement(key);
        if (e != null) {
            scrollTo(e.getLocation().getX(), e.getLocation().getY() - 100);
        }
        return e;
    }

    private WebElement scrollToElementToBeVisible(WebElement element) {
        if (element != null) {
            scrollTo(element.getLocation().getX(), element.getLocation().getY() - 100);
        }
        return element;
    }

    private void hoverElementBy(String key) {
        WebElement webElement = findElement(key);
        action.moveToElement(webElement).build().perform();
    }

    private void hoverElement(WebElement element) {
        action.moveToElement(element).build().perform();
    }

    private WebElement findElementWithKey(String key) {
        return findElement(key);
    }

    public String randomString(int stringLength) {
        Random random = new Random();
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUWVXYZabcdefghijklmnopqrstuwvxyz0123456789".toCharArray();
        String stringRandom = "";

        for (int i = 0; i < stringLength; ++i) {
            stringRandom = stringRandom + chars[random.nextInt(chars.length)];
        }

        return stringRandom;
    }

    public boolean isChecked(String key) {
        if (this.isElementPresent((String) key, 10)) {
            WebElement element = this.findElement(key);
            return this.isChecked(element);
        } else {
            new Exception("Could not identify element with key: " + key);
            return false;
        }
    }

    public boolean isChecked(WebElement element) {
        return element.isSelected();
    }

    public boolean isChecked(WebElement element, String key) {
        boolean isChecked;
        try {
            isChecked = this.isChecked(element);
        } catch (StaleElementReferenceException var5) {
            if (key == null) {
                var5.printStackTrace();
                new Exception("StaleElementReferenceException within isChecked");
            }

            element = this.findElement(key);
            isChecked = this.isChecked(element);
        }

        return isChecked;
    }

    public boolean isElementPresent(String key, int timeoutSeconds) {
        if (key != null && !key.isEmpty()) {
            By by = this.getElementInfoToBy(this.findElementInfoByKey(key));
            return this.isElementPresent(by, timeoutSeconds);
        } else {
            logger.warn("Empty locator has been passed to field detection");
            return false;
        }
    }

    public boolean isElementPresent(String key) {
        if (key != null && !key.isEmpty()) {
            By by = this.getElementInfoToBy(this.findElementInfoByKey(key));
            return this.isElementPresent(by);
        } else {
            logger.warn("Empty locator has been passed to field detection");
            return false;
        }
    }

    private boolean isElementPresent(By by, int timeoutSeconds) {
        FluentWait wait = (new FluentWait(by)).withTimeout(Duration.ofSeconds((long) timeoutSeconds)).pollingEvery(Duration.ofMillis(250L)).ignoring(NoSuchElementException.class);

        try {
            wait.until(new Function<By, Boolean>() {
                public Boolean apply(By by) {
                    boolean isFound = false;

                    try {
                        Driver.driver.findElement(by);
                        isFound = true;
                    } catch (Exception var4) {
                        isFound = false;
                    }

                    return isFound;
                }
            });
            return true;
        } catch (Throwable var5) {
            return false;
        }
    }

    public boolean isElementPresent(By by) {
        WebElement webElement = null;

        try {
            webElement = driver.findElement(by);
        } catch (Exception var4) {
        }

        return webElement != null;
    }

    private void sleepSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            logger.info("Waited " + seconds + " seconds ");
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public void javaScriptClicker(WebElement element) {
        JavascriptExecutor jse = ((JavascriptExecutor) driver);
        jse.executeScript("var evt = document.createEvent('MouseEvents');"
                + "evt.initMouseEvent('click',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
                + "arguments[0].dispatchEvent(evt);", element);
    }

    private WebElement randomPick(String key) {
        List<WebElement> elements = findElements(key);
        Random random = new Random();
        int index = random.nextInt(elements.size());
        return elements.get(index);
    }

    private void selectByVisibleText(String key, String text) {
        Select select = new Select(findElement(key));
        select.selectByVisibleText(text);
        logger.info("Selected from dropdown");
    }

    public String getAttribute(String key, String attribute) {
        return this.getAttribute(this.findElement(key), attribute);
    }

    public String getAttribute(WebElement element, String attribute) {
        Actions builder = new Actions(driver);

        try {
            builder.moveToElement(element).build().perform();
        } catch (Exception var5) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(false);", new Object[]{element});
        }

        String retVal = element.getAttribute(attribute);
        return retVal;
    }

    public int createHucAndGetResponseCode(String url) {
        HttpURLConnection huc;
        try {
            HttpURLConnection.setFollowRedirects(false);
            huc = (HttpURLConnection) (new URL(url).openConnection());
            huc.setRequestMethod("GET");
            huc.connect();
            return huc.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String getValueAttributeText(String key) {
        return this.getAttribute(key, "value");
    }

    public String getIfValueInSavekey(String value) {
        String valueInSavekey;
        try {
            valueInSavekey = this.getValue(value);
            logger.info(value + " key ile saveKey icerisinde deger bulundu.(" + valueInSavekey + ")");
        } catch (NullPointerException var4) {
            valueInSavekey = value;
            logger.info(value + " key ile saveKey icerisinde deger bulunamadi.Degerin kendisiyle devam ediliyor...(" + value + ")");
        }

        return valueInSavekey;
    }

    @Step({"Toplam sekme sayisi"})
    public void totalTabs() {
        Set<String> allWindows = driver.getWindowHandles();
        System.out.println("totalTabs: " + allWindows.size());
    }

    @Step({"Sekmelerin idlerini goster"})
    public void totalTabsIds() {
        Set<String> allWindows = driver.getWindowHandles();
        Iterator<String> i = allWindows.iterator();
        for (int j = 1; i.hasNext(); ++j) {
            String childwindow = (String) i.next();
            System.out.println(j + ". sekme: " + childwindow.toString());
        }
    }

    @Step({"Ilk windowa don"})
    public void returnFirstWindow() {
        Set<String> allWindows = driver.getWindowHandles();
        Iterator<String> i = allWindows.iterator();
        if (i.hasNext()) {
            String childwindow = (String) i.next();
            driver.switchTo().window(childwindow);
        }
    }

    @Step({"<sekme> .sekmeye(tab) don"})
    public void returnSelectedTab(Integer sekme) {
        Set<String> allWindows = driver.getWindowHandles();
        Iterator<String> i = allWindows.iterator();
        ArrayList<String> tabName = new ArrayList();
        while (i.hasNext()) {
            String childwindow = (String) i.next();
            tabName.add(childwindow);
        }
        driver.switchTo().window((String) tabName.get(sekme - 1));
    }

    @Step({"Hover by given element <key>"})
    public void hoverByGivenElement(String key) {
        hoverElementBy(key);
    }

    @Step({"<key> li elementin isaretli(checked) oldugunu dogrula"})
    public void elementIsChecked(String key) {
        boolean isChecked = this.isChecked(key);
        assertTrue(isChecked);
        logger.info("'" + key + "' li element isaretli olarak goruntulenmistir.");
    }

    @Step({"<key> li elementin isaretli(checked) olmadıgını dogrula"})
    public void elementIsNotChecked(String key) {
        boolean isChecked = this.isChecked(key);
        assertTrue(!isChecked);
        logger.info("'" + key + "' li element isaretli olarak goruntulenmemistir.");
    }

    @Step({"Wait <value> milliseconds"})
    public void waitByMilliSeconds(long milliseconds) {
        try {
            logger.info(milliseconds + " milisaniye bekleniyor.");
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Step({"Wait <value> seconds"})
    public void waitBySeconds(int seconds) {
        logger.info(seconds + " saniye bekleniyor.");
        sleepSeconds(seconds);
    }

    @Step({"Click to element <key>", "Elementine tıkla <key>"})
    public void clickElement(String key) {
        if (!key.equals("")) {
            WebElement element = findElement(key);
            hoverElement(element);
            waitByMilliSeconds(500);
            clickElement(element);
            logger.info(key + " elementine tiklandi.");
        }
    }

    @Step({"Hover all elements in <key> with js"})
    public void hoverAllMenuLinksWithJs(String key) {
        List<WebElement> elements = findElements(key);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (WebElement element : elements) {
            js.executeScript("var element = arguments[0];"
                    + "var mouseEventObj = document.createEvent('MouseEvents');"
                    + "mouseEventObj.initEvent( 'mouseover', true, true );"
                    + "element.dispatchEvent(mouseEventObj);", element);
        }
    }

    @Step({"Click with javascript to xpath <xpath>",
            "Javascript ile xpath tıkla <xpath>"})
    public void javascriptClickerWithXpath(String xpath) {
        assertTrue("Element bulunamadı", isDisplayedBy(By.xpath(xpath)));
        javaScriptClicker(driver.findElement(By.xpath(xpath)));
        logger.info("Javascript ile " + xpath + " tıklandı.");
    }

    @Step({"Click with javascript to css <css>",
            "Javascript ile css tıkla <css>"})
    public void javascriptClickerWithCss(String css) {
        assertTrue("Element bulunamadi", isDisplayedBy(By.cssSelector(css)));
        javaScriptClicker(driver.findElement(By.cssSelector(css)));
        logger.info("Javascript ile " + css + " tiklandi.");
    }

    @Step({"Check if element <key> contains text <expectedText>",
            "<key> elementi <text> degerini iceriyor mu kontrol edilir"})
    public void checkElementContainsText(String key, String expectedText) {
        boolean containsText = findElement(key).getText().contains(expectedText);
        assertTrue("Expected text is not contained", containsText);
        logger.info(key + " elementi" + expectedText + "degerini iceriyor.");
    }

    @Step({"Check if element <key> not contains text <expectedText>",
            "<key> elementi <text> degerini icermedigi dogrulanir"})
    public void checkElementNotContainsText(String key, String expectedText) {
        String text = findElement(key).getText();
        assertFalse("Expected text is contain", text.contains(expectedText));
        logger.info(key + " elementi" + expectedText + "degerini icermiyor.");
    }

    @Step({"Check if title contains text <expectedText>",
            "Title <text> degerini iceriyor mu kontrol et"})
    public void checkTitleContainsText(String expectedText) {
        boolean containsText = getTitle().contains(expectedText);
        assertTrue("Title not contain expected text", containsText);
        logger.info(" Title" + expectedText + "degerini iceriyor.");
    }

    @Step("Click with js method <key>")
    public void jsClicker(String key) {
        WebElement element = findElement(key);
        JavascriptExecutor jse = ((JavascriptExecutor) driver);
        jse.executeScript("var evt = document.createEvent('MouseEvents');"
                + "evt.initMouseEvent('click',true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0,null);"
                + "arguments[0].dispatchEvent(evt);", element);
    }

    @Step({"<key> alanına kaydır"})
    public void scrollToElement(String key) {
        scrollToElementToBeVisible(key);
        logger.info(key + " elementinin oldugu alana kaydirildi");
    }

    @Step({"<key> menu listesinden rasgele sec"})
    public void chooseRandomElementFromList(String key) {
        clickElement(randomPick(key));
    }

    @Step({"Navigate to home page"})
    public void navigateToHomePage() {
        driver.navigate().to(System.getenv("APP_URL"));
        logger.info("Navigated to homepage ");
    }

    @Step({"Navigate to given page <string>"})
    public void navigateToGivenPage(String string) {
        driver.navigate().to(string);
        logger.info("Navigated to given url :" + string);
    }

    @Step({"Select from <key> list with <text>"})
    public void select(String key, String text) {
        selectByVisibleText(key, text);
    }

    @Step("Check broken links with <key> element")
    public void brokenLinkCheck(String key) {
        String url;
        int respCode;
        HttpURLConnection huc;
        Iterator<WebElement> it = findElements(key).iterator();
        while (it.hasNext()) {
            url = getAttribute(it.next(), "href");
            if (!url.startsWith(System.getenv("APP_URL"))) continue;
            if (url.isEmpty()) continue;
            respCode = createHucAndGetResponseCode(url);
            softAssertions.assertThat(respCode).isLessThan(400);
        }
    }

    @Step({"Send <text> Keys to given element <key>"})
    public void sendKeysToGivenElement(String text, String key) {
        sendKeys(text, key);
        logger.info(key + " elementinin oldugu alana kaydirildi");
    }

    @Step({"<text> textini <key> elemente tek tek yaz"})
    public void sendKeyOneByOne(String text, String key) throws InterruptedException {
        WebElement field = this.findElement(key);
        field.clear();
        if (!key.equals("")) {
            char[] var4 = text.toCharArray();
            int var5 = var4.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                char ch = var4[var6];
                this.findElement(key).sendKeys(new CharSequence[]{Character.toString(ch)});
            }

            Thread.sleep(10L);
            this.logger.info(key + " elementine " + text + " texti karakterler tek tek girlilerek yazıldı.");
        }

    }

    @Step({"<keyUsername> ve <keyPassword> elementlerine <myUsername> ve <myPassword> bilgilerini gir"})
    public void myLogin(String keyUsername, String keyPassword, String myUsername, String myPassword) {
        this.findElement(keyUsername).sendKeys(new CharSequence[]{myUsername});
        this.findElement(keyPassword).sendKeys(new CharSequence[]{myPassword});
    }

    @Step({"<key> li elementin textini <saveKey> olarak kaydet"})
    public void getElementTextAndSave(String key, String saveKey) {
        String text = this.getText(key);
        this.saveValue(saveKey, text);
    }

    @Step({"<saveKey> saveKey olarak saklanan degeri info olarak logla"})
    public void infoLogSaveKeyValue(String saveKey) {
        logger.info(saveKey);
    }

    @Step({"<value> ifadesini info olarak logla"})
    public void infoLog(String value) {
        logger.info(value);
    }

    @Step({"<saveKey> olarak saklanan degeri konsola yazdir"})
    public void writeToConsoleSavedKey(String saveKey) {
        this.writeToConsole(this.getValue(saveKey));
    }

    @Step({"<key> degerini console ekranina yaz"})
    public void writeToConsole(String key) {
        System.out.println(key);
    }

    @Step({"<saveKey> olarak saklanan deger degerin null olmadigini dogrula"})
    public void verifyNotNullSavedKeyValue(String saveKey) {
        assertFalse("'" + saveKey + "' olarak saklanan degerin null olmadigi dogrulanamamıstir.", this.getValue(saveKey).isEmpty());
    }

    public boolean verifyCompareSavedKeyWithText(String saveKey, String expectedValue) {
        return this.getValue(saveKey).equalsIgnoreCase(expectedValue);
    }

    @Step({"<saveKey> olarak saklanan degeri <expectedValue> degeri ile dogrula"})
    public void verifySavedKeyValue(String saveKey, String expectedValue) {
        assertTrue("'" + saveKey + "' olarak saklanan deger, '" + expectedValue + "' degeri ile dogrulanamamistir.", this.getValue(saveKey).equalsIgnoreCase(expectedValue));

    }

    @Step({"<key> li elementin degerinin <value> attribute degeri ile aynı oldugunu dogrula"})
    public void compareElementValueToString(String key, String value) throws Exception {
        String txt = this.getValueAttributeText(key);
        String valueInSavekey = this.getIfValueInSavekey(value);

        try {
            assertTrue(txt.compareTo(valueInSavekey) == 0);
            logger.info("'" + key + "' li elementin degeri beklendigi sekilde '" + value + "' olarak goruntulenmistir.");
        } catch (Exception var6) {
            new Exception("'" + key + "' li elementin degeri beklenmedik sekilde hatalı olarak goruntulenmistir.\nBeklenilen deger: '" + value + "'\nGoruntulenen deger: '" + txt + "'\n");
        }
    }

    @Step({"<key> li elementin textinin <text> degeri ile aynı oldugunu dogrula"})
    public void compareElementTextToString(String key, String text) {
        String txt = this.getText(key);
        String valueInSavekey = this.getIfValueInSavekey(text);

        try {
            assertTrue(txt.compareTo(valueInSavekey) == 0);
            logger.info("'" + key + "' li elementin texti beklendigi sekilde '" + text + "' olarak görüntülenmistir.");
        } catch (Exception var6) {
            new Exception("'" + key + "' li elementin texti beklenmedik sekilde hatalı olarak görüntülenmistir.\nBeklenilen text: '" + text + "'\nGörüntülenen text: '" + txt + "'\n");
        }
    }

    @Step({"<key> li elementin textinin <saveKey> olarak saklanan deger ile aynı oldugunu dogrula"})
    public void compareElementTextToSavedKey(String key, String saveKey) {
        String txt = this.getText(key);
        String valueInSavekey = getValue(saveKey);
        System.out.println("txt: " + txt);
        System.out.println("valueInSavekey: " + valueInSavekey);
        try {
            assertTrue(txt.compareTo(valueInSavekey) == 0);
            logger.info("'" + key + "' li elementin texti beklendigi sekilde '" + saveKey + "' olarak görüntülenmistir.");
        } catch (Exception var6) {
            new Exception("'" + key + "' li elementin texti beklenmedik sekilde hatalı olarak görüntülenmistir.\nBeklenilen text: '" + saveKey + "'\nGörüntülenen text: '" + txt + "'\n");
        }
    }

    @Step({"<length> uzunlugunda random bir kelime üret ve <saveKey> olarak sakla"})
    public void createRandomString(int length, String saveKey) {
        this.saveValue(saveKey, this.randomString(length));
    }

    ////////////////////////////berkay///////////////////////
////////////////////////////berkay///////////////////////
////////////////////////////berkay///////////////////////
////////////////////////////berkay///////////////////////


    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////

    public boolean isDisplayed(String key) {
        boolean retValue = false;
        if (this.isElementPresent(key)) {
            WebElement element = this.findElement(key);

            try {
                retValue = element.isDisplayed();
            } catch (Exception var7) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500L);
                } catch (InterruptedException var6) {
                }

                retValue = element.isDisplayed();
            }
        }

        return retValue;
    }

    @Step({"<key> li elementin ekranda goruntulendigini dogrula"})
    public void elementIsDisplayed(String key) {
        boolean isDisplayed = this.isDisplayed(key);
        assertTrue(isDisplayed);
        logger.info("'" + key + "' li element ekranda gorüntulenmistir.");
    }

    @Step({"<key> li elementin ekranda goruntulenmedigini dogrula"})
    public void elementIsNotDisplayed(String key) {
        boolean isDisplayed = this.isDisplayed(key);
        assertFalse(isDisplayed);
        logger.info("'" + key + "' li element ekranda goruntulenmemistir.");
    }

    @Step({"<key> li element var ise tıkla"})
    public void clickElementIfExists(String key) {
        boolean isExists = this.clickIfExist(key);
        if (!isExists) {
            logger.info("'" + key + "' li element ekranda görüntülenmediğinden tıklanamadı.");
        }

    }

    public boolean clickIfExist(String key) {
        if (this.isElementPresent(key)) {
            this.clickElement(key);
            return true;
        } else {
            return false;
        }
    }

    @Step({"<key> li alanı temizle"})
    public void clearToElement(String key) {
        this.clearTextArea(key);
    }

    @Step({"<saveKey1> ve <saveKey2> degerlerini dogrula"})
    public void verifySaveKeyValues(String saveKey1, String saveKey2) {
        String firstValue = this.getValue(saveKey1);
        String secondValue = this.getValue(saveKey2);
        assertEquals("SaveKey degerleri dogrulanamadi." + firstValue + "-" + secondValue, firstValue, secondValue);
    }

    @Step({"<saveKey1> ve <saveKey2> degerlerinin esit olmadigini dogrula"})
    public void verifySaveKeyValuesIsNotEquals(String saveKey1, String saveKey2) {
        String firstValue = this.getValue(saveKey1);
        String secondValue = this.getValue(saveKey2);
        assertFalse("SaveKey degerlerinin esit olmadigi dogrulanamadi." + firstValue + "-" + secondValue, firstValue.equals(secondValue));
    }


    private Boolean isEditable(String key) {
        logger.info("Checking is element enable on the page ");
        return findElement(key).isEnabled();
    }

    private boolean isDisplayedBy(By by) {
        return driver.findElement(by).isDisplayed();

    }

    @Step({"<key> has next degeri true ise her sayfada bulunan <elements> li key ile 60 urun yuklendigini dogrula"})
    public void validateProductsSizeEachScrollDown(String key, String elements) throws InterruptedException {
        List<WebElement> listOfElements = null;
        final int itemSizeInEveryPage = 60;
        int tempCounter = 0;
        int page = 0;
        int index = 0;
        int listSize;
        while (getAttribute(key, "value").equals("True")) {
            if (page == 16) break;
            page++;
            index += itemSizeInEveryPage;
            listOfElements = findElements(elements);
            listSize = listOfElements.size();
            assertEquals(itemSizeInEveryPage, (listSize - tempCounter));
            scrollToElementToBeVisible(listOfElements.get(index - 1));
            tempCounter = listSize;
            sleepSeconds(2);
        }
    }


    @Step({"Select address from auto suggestive dropdown with <key>"})
    public void selectFromAutoSuggestiveDropDown(String key) {
        String optionToSelect = "Mexico City, CDMX, Mexico";
        List<WebElement> contentList = findElements(key);
        WebElement tempElement = null;
        for (WebElement e : contentList) {
            String currentOption = e.getAttribute("textContent");
            if (currentOption.equals(optionToSelect)) {
                tempElement = e.findElement(By.xpath("//parent::div"));
                // Actions ac = new Actions(driver);
                //ac.moveToElement(tempElement).click().build().perform();
                clickElement(tempElement);
                break;
            }
        }
    }


    @Step({"Element with <key> is displayed on website",
            "<key> li element sayfada goruntuleniyor mu kontrol et "})
    public void isDisplayedSection(String key) {
        WebElement element = findElement(key);
        assertTrue("Section bulunamadı", element.isDisplayed());
        logger.info(key + "li section sayfada bulunuyor.");
    }

    @Step({"Element with <key> is enable on website",
            "<key> li element sayfada enable mı kontrol et "})
    public void isEnableSection(String key) {
        WebElement element = findElement(key);
        assertTrue("Section bulunamadı", element.isEnabled());
        logger.info(key + "li section sayfada bulunuyor.");
    }


    @Step("Check if new tab has verified url <key>")
    public void switchTabsUsingPartOfUrl(String key) {
        String currentHandle = null;
        try {
            final Set<String> handles = driver.getWindowHandles();
            if (handles.size() > 1) {
                currentHandle = driver.getWindowHandle();
            }
            if (currentHandle != null) {
                for (final String handle : handles) {
                    driver.switchTo().window(handle);
                    if (driver.getCurrentUrl().contains(key) && !currentHandle.equals(handle)) {
                        break;
                    }
                }
            } else {
                for (final String handle : handles) {
                    driver.switchTo().window(handle);
                    if (driver.getCurrentUrl().contains(key)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Switching tabs failed");
        }
    }

    @Step({"Scroll to <key> field",
            "<key> alanına js ile kaydır"})
    public void scrollToElementWithJs(String key) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", findElement(key));
        waitByMilliSeconds(200);
    }

    @Step({"Check if element <key> exists else print message <message>",
            "Element <key> var mı kontrol et yoksa hata mesajı ver <message>"})
    public void getElementWithKeyIfExistsWithMessage(String key, String message) {
        int loopCount = 0;
        while (loopCount < DEFAULT_MAX_ITERATION_COUNT) {
            if (findElements(key).size() > 0) {
                logger.info(key + " elementi bulundu.");
                return;
            }
            loopCount++;
            waitByMilliSeconds(DEFAULT_MILLISECOND_WAIT_AMOUNT);
        }
        Assert.fail(message);
    }


    @Step("Check position <key1> relative to <key2>")
    public Boolean checkRelativePosition(String key1, String key2) {
        WebElement parent = findElement(key1);
        WebElement child = findElement(key2);
        boolean isAbove = false;
        if (!(parent.getLocation() == null && child.getLocation() == null)) {
            if (parent.getLocation().getY() - child.getLocation().getY() < 0) {
                isAbove = true;
                logger.info(key1 + " element is above the" + key2);
            }

            if (parent.getLocation().getY() - child.getLocation().getY() > 0) {
                isAbove = false;
                logger.info(key1 + " element is under the " + key2);
            }
        }
        return isAbove;
    }

    @Step({"Check if element <key> has attribute <attribute>",
            "<key> elementi <attribute> niteligine sahip mi"})
    public void checkElementAttributeExists(String key, String attribute) {
        WebElement element = findElement(key);
        int loopCount = 0;
        while (loopCount < DEFAULT_MAX_ITERATION_COUNT) {
            if (element.getAttribute(attribute) != null) {
                logger.info(key + " elementi " + attribute + " niteligine sahip.");
                return;
            }
            loopCount++;
            waitByMilliSeconds(DEFAULT_MILLISECOND_WAIT_AMOUNT);
        }
        Assert.fail("Element DOESN't have the attribute: '" + attribute + "'");
    }

    @Step("Wait until image uploaded and click checkbox <key>")
    public void waitUntilImageUploadedAndClickCheckBox(String key) {
        WebDriverWait wait = new WebDriverWait(driver, 15);
        boolean elm = wait.until(ExpectedConditions.attributeContains(By.cssSelector("img[class='custom-image__uploaded-image']"), "src", "image/"));
        if (elm) findElement(key).click();
    }

    @Step("Wait until elements loaded <key> and verify sorting")
    public void waitUntilElementsLoadedAndCheckSortingSuccessful(String key) {
        By infoParam = this.getElementInfoToBy(this.findElementInfoByKey(key));
        WebDriverWait wait = new WebDriverWait(driver, 15);
        List<WebElement> elements = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(infoParam, 59));
        Integer[] actual = new Integer[elements.size()];
        Integer[] sorted = new Integer[elements.size()];
        int i = 0;
        for (WebElement e : elements) {
            actual[i] = sorted[i] = Integer.parseInt((e.getText()).replace(",", ""));
            i++;
        }
        Arrays.sort(sorted, Collections.reverseOrder());
        for (int k = 0; k < elements.size(); k++) {
            assertEquals(actual[k], sorted[k]);
        }
    }

    @Step("<key> product is exist on cart page")
    public void isExistOnCartPage(String key) {
        assertTrue(findElements(key).size() > 0);
    }

    @Step("<key> product is not exist on cart page")
    public void isNotExistOnCartPage(String key) {
        int b= findElements(key).size();
        boolean a = findElements(key).size() > 0;
        assertFalse(findElements(key).size() > 0);
    }

}

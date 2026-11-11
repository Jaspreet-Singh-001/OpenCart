package utilities;

import java.io.IOException;

import org.testng.annotations.DataProvider;

public class DataProviders {

    private static final String DATA_FILE = ".\\testData\\OpenCartLoginData.xlsx";
    private static final String LOGIN_SHEET = "Sheet2";

    @DataProvider(name = "LoginData")
    public String[][] getLoginData() throws IOException {
        ExcelUtility excel = new ExcelUtility(DATA_FILE);
        int rowCount = excel.getRowCount(LOGIN_SHEET);
        int columnCount = excel.getCellCount(LOGIN_SHEET, 1);
        String[][] loginData = new String[rowCount][columnCount];

        // Row 0 contains the column headings, so data starts on row 1.
        for (int row = 1; row <= rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                loginData[row - 1][column] =
                        excel.getCellData(LOGIN_SHEET, row, column);
            }
        }

        return loginData;
    }
}

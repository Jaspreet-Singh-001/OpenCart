package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtility {

    private static final int WRITE_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MILLIS = 250;

    private final String filePath;

    public ExcelUtility(String path) {
        this.filePath = path;
    }

    public int getRowCount(String sheetName) throws IOException {
        try (FileInputStream input = new FileInputStream(filePath);
                XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            return workbook.getSheet(sheetName).getLastRowNum();
        }
    }

    public int getCellCount(String sheetName, int rowNumber) throws IOException {
        try (FileInputStream input = new FileInputStream(filePath);
                XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            return workbook.getSheet(sheetName).getRow(rowNumber).getLastCellNum();
        }
    }

    public String getCellData(String sheetName, int rowNumber, int columnNumber)
            throws IOException {
        try (FileInputStream input = new FileInputStream(filePath);
                XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            XSSFCell cell = workbook.getSheet(sheetName)
                    .getRow(rowNumber)
                    .getCell(columnNumber);
            return new DataFormatter().formatCellValue(cell);
        }
    }

    public void setCellData(String sheetName, int rowNumber, int columnNumber, String data)
            throws IOException {
        writeCells(sheetName, rowNumber, columnNumber, data);
    }

    /** Writes all supplied values to consecutive cells using one workbook save. */
    public void setRowData(String sheetName, int rowNumber, String... values)
            throws IOException {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one cell value is required");
        }
        writeCells(sheetName, rowNumber, 0, values);
    }

    private void writeCells(String sheetName, int rowNumber, int firstColumn,
            String... values) throws IOException {
        File excelFile = new File(filePath).getAbsoluteFile();
        Path excelPath = excelFile.toPath();
        Path parent = excelPath.getParent();
        Files.createDirectories(parent);

        IOException lastException = null;
        for (int attempt = 1; attempt <= WRITE_ATTEMPTS; attempt++) {
            Path temporaryFile = Files.createTempFile(
                    parent, excelFile.getName(), ".tmp");
            try {
                try (XSSFWorkbook workbook = openWorkbook(excelFile)) {
                    XSSFSheet sheet = workbook.getSheet(sheetName);
                    if (sheet == null) {
                        sheet = workbook.createSheet(sheetName);
                    }

                    XSSFRow row = sheet.getRow(rowNumber);
                    if (row == null) {
                        row = sheet.createRow(rowNumber);
                    }

                    for (int i = 0; i < values.length; i++) {
                        int column = firstColumn + i;
                        Cell cell = row.getCell(column);
                        if (cell == null) {
                            cell = row.createCell(column);
                        }
                        cell.setCellValue(values[i]);
                    }

                    try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                        workbook.write(output);
                    }
                }

                replaceWorkbook(temporaryFile, excelPath);
                return;

            } catch (IOException e) {
                lastException = e;
                if (attempt == WRITE_ATTEMPTS) {
                    break;
                }
                waitBeforeRetry(attempt, e);

            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }

        throw new IOException("Could not save Excel workbook after "
                + WRITE_ATTEMPTS + " attempts. Close the workbook in Excel or any preview, "
                + "then retry.", lastException);
    }

    private XSSFWorkbook openWorkbook(File excelFile) throws IOException {
        if (!excelFile.exists()) {
            return new XSSFWorkbook();
        }
        try (InputStream input = new FileInputStream(excelFile)) {
            return new XSSFWorkbook(input);
        }
    }

    private void replaceWorkbook(Path temporaryFile, Path excelPath) throws IOException {
        try {
            Files.move(temporaryFile, excelPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
            Files.move(temporaryFile, excelPath,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void waitBeforeRetry(int attempt, IOException originalException)
            throws IOException {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrying the Excel workbook save",
                    originalException);
        }
    }
}
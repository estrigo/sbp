package kz.spt.whitelistplugin.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.model.Whitelist;
import lombok.experimental.UtilityClass;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@UtilityClass
public class ExcelUtils {

    public static List<Pair<Whitelist, String>> parseExcelFileWhiteList(InputStream is, Parking parking) {

        DataFormatter formatter = new DataFormatter();
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet != null) {
                Iterator<Row> rows = sheet.iterator();

                List<Pair<Whitelist, String>> lstWhitelist = new ArrayList<>();

                int rowNumber = 0;
                while (rows.hasNext()) {
                    Row currentRow = rows.next();

                    // skip header
                    if (rowNumber == 0) {
                        rowNumber++;
                        continue;
                    }

                    Iterator<Cell> cellsInRow = currentRow.iterator();

                    Whitelist whitelist = new Whitelist();
                    Cars cars = new Cars();
                    whitelist.setParking(parking);
                    String groupName = "";
                    int cellIndex = 0;
                    while (cellsInRow.hasNext()) {
                        Cell currentCell = cellsInRow.next();
                        if (cellIndex == 0) { // plate number
                            cars.setPlatenumber(formatter.formatCellValue(currentCell));
                            whitelist.setPlatenumber(cars.getPlatenumber());
                        }
                        if (cellIndex == 1) { // full name
                            whitelist.setFullName(formatter.formatCellValue(currentCell));
                        }
                        if (cellIndex == 2) { // address
                            whitelist.setAddress(formatter.formatCellValue(currentCell));
                        }
                        if (cellIndex == 3) { // parking number
                            whitelist.setParkingNumber(formatter.formatCellValue(currentCell));
                        }
                        if (cellIndex == 4) { // comments
                            whitelist.setComment(formatter.formatCellValue(currentCell));
                        }
                        if (cellIndex == 5) { // group name
                            groupName = formatter.formatCellValue(currentCell);
                        }
                        cellIndex++;
                    }

                    lstWhitelist.add(new Pair<>(whitelist, groupName));
                }

                // Close WorkBook
                workbook.close();

                return lstWhitelist;
            }
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }
}
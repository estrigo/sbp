package kz.spt.whitelistplugin.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@UtilityClass
public class ExcelUtils {

    public static List<Whitelist> parseExcelFileWhiteList(InputStream is, Parking parking) {

        DataFormatter formatter = new DataFormatter();
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet != null) {
                Iterator<Row> rows = sheet.iterator();

                List<Whitelist> lstWhitelist= new ArrayList<Whitelist>();

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
                    int cellIndex = 0;
                    while (cellsInRow.hasNext()) {
                        Cell currentCell = cellsInRow.next();
                        if (cellIndex == 0) { // plate number
                            cars.setPlatenumber(formatter.formatCellValue(currentCell));
                            whitelist.setPlatenumber(cars.getPlatenumber());
                        }

                        cellIndex++;
                    }

                    lstWhitelist.add(whitelist);
                }

                // Close WorkBook
                workbook.close();

                return lstWhitelist;
            }
            return new ArrayList<Whitelist>();
        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }


    public static List<String> parseExcelFileWhiteListGroups(InputStream is, Parking parking) {

        DataFormatter formatter = new DataFormatter();
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet != null) {
                Iterator<Row> rows = sheet.iterator();

                List<String> groups= new ArrayList<String>();

                int rowNumber = 0;
                while (rows.hasNext()) {
                    Row currentRow = rows.next();

                    // skip header
                    if (rowNumber == 0) {
                        rowNumber++;
                        continue;
                    }

                    Iterator<Cell> cellsInRow = currentRow.iterator();

                    int cellIndex = 0;
                    String groupName = "";
                    while (cellsInRow.hasNext()) {
                        Cell currentCell = cellsInRow.next();

                         if(cellIndex==1) { // group name
                            groupName = formatter.formatCellValue(currentCell);
                        }

                        cellIndex++;
                    }


                    groups.add(groupName);
                }


                // Close WorkBook
                workbook.close();

                return groups;
            }
            return new ArrayList<String>();
        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }
}
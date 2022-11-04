package kz.spt.carmodelplugin.utils;

import kz.spt.lib.model.CarModel;
import kz.spt.lib.model.Dimensions;
import lombok.experimental.UtilityClass;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class ExcelUtilsCarModel {

    public static List<Pair<CarModel, String>> parseExcelFileWhiteList(InputStream is, Dimensions dimensions) {

        DataFormatter formatter = new DataFormatter();
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet != null) {
                Iterator<Row> rows = sheet.iterator();

                List<Pair<CarModel, String>> lstCarModellist= new ArrayList<>();

                int rowNumber = 0;
                while (rows.hasNext()) {
                    Row currentRow = rows.next();

                    // skip header
                    if (rowNumber == 0) {
                        rowNumber++;
                        continue;
                    }

                    Iterator<Cell> cellsInRow = currentRow.iterator();

                    CarModel carModel = new CarModel();
                    Dimensions dimensions1 = new Dimensions();
                    carModel.setDimensions(dimensions);
                    String groupName = "";
                    int cellIndex = 0;
                    while (cellsInRow.hasNext()) {
                        Cell currentCell = cellsInRow.next();
                        if (cellIndex == 0) { // plate number
                            carModel.setModel(formatter.formatCellValue(currentCell));
                        }

                        cellIndex++;
                    }

                    lstCarModellist.add(new Pair<>(carModel, groupName));
                }

                // Close WorkBook
                workbook.close();

                return lstCarModellist;
            }
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }
}
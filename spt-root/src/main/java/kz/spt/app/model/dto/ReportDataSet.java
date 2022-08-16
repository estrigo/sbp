package kz.spt.app.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportDataSet {

   private List<?> dataSet;

   public ReportDataSet() {
   }

   public ReportDataSet(List<?> dataSet) {
      this.dataSet = dataSet;
   }
}

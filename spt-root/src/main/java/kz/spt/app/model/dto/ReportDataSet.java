package kz.spt.app.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportDataSet {

   private List<?> dataSet;

   private Object object;

   public ReportDataSet() {
   }

   public ReportDataSet(List<?> dataSet) {
      this.dataSet = dataSet;
   }

   public ReportDataSet(Object object) {
      this.object = object;
   }
}

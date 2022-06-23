package kz.spt.lib.bootstrap.datatable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
@Setter
@Getter
@NoArgsConstructor
public class PagingRequest {

    private int start;
    private int length;
    private int draw;
    private List<Order> order;
    private List<Column> columns;
    private Search search;
    private Map<String, String> customFilters;

    public <T> T convertTo(T object){
        if(this.customFilters.isEmpty()) return object;

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            String value = this.customFilters.get(field.getName());
            if(value == null || value.isEmpty()) continue;

            try {
                if (Long.class.equals(field.getType())) {
                    field.set(object, Long.valueOf(value));
                } else if (BigDecimal.class.equals(field.getType())) {
                    field.set(object, new BigDecimal(value));
                }else if(Date.class.equals(field.getType())){
                    if(value.length() == 10){ // 2022-05-04
                        field.set(object, new SimpleDateFormat("yyyy-MM-dd").parse(value));
                    } else if(value.length() == 16){
                        field.set(object, new SimpleDateFormat("yyyy-MM-dd'T'hh:mm").parse(value));
                    } else if(value.length() == 19){ //2022-05-27T11:00:00
                        field.set(object, new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(value));
                    } else if(value.length() == 23){ //2022-05-27T11:00:00.000
                        field.set(object, new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(value));
                    }
                }else if(boolean.class.equals(field.getType())){
                    field.set(object, Boolean.valueOf(value).booleanValue());
                }
                else{
                    field.set(object, value);
                }
            } catch (IllegalAccessException | ParseException e) {
                e.printStackTrace();
            }
        }

        return object;
    }
}

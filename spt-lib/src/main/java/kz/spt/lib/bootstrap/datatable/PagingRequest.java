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
import org.pf4j.util.StringUtils;

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
            Object value = this.customFilters.get(field.getName());
            if(value == null || "".equals(value)) continue;
            try {
                if(boolean.class.equals(field.getType())){
                    field.set(object, Boolean.valueOf(String.valueOf(value)));
                } else if(Double.class.equals(field.getType())){
                    field.set(object, Double.valueOf(String.valueOf(value)));
                } else if(Integer.class.equals(field.getType())){
                    field.set(object, Double.valueOf(String.valueOf(value)).intValue());
                } else {
                    field.set(object, convertInstanceOfObject(value, field.getType()));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    private static <T> T convertInstanceOfObject(Object o, Class<T> clazz) {
        try {
            return clazz.cast(o);
        } catch(ClassCastException e) {
            return null;
        }
    }
}

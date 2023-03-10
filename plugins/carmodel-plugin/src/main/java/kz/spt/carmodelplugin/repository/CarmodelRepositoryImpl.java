package kz.spt.carmodelplugin.repository;

import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.model.Cars;
import lombok.extern.java.Log;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
@Repository("carmodelRepository")
public class CarmodelRepositoryImpl implements CarmodelRepository {

    @PersistenceContext
    private EntityManager em;

    private Query getQuery(String queryHead, String platenumber, String dateFrom) {

        String query = queryHead + " from cars c " +
                " left join car_model cm on c.model=cm.model " +
                " left join car_state cs on cs.car_number=c.platenumber " +
                " where cs.in_timestamp is not null";
        if (platenumber != null && platenumber != "") {
            query += " and c.platenumber=:platenumber ";
        }
        if (dateFrom != null && dateFrom != "") {
            query += " and cs.in_timestamp >=:dateFrom ";
        }
        query += " group by c.platenumber  " +
                " order by cs.id desc";

        Query q = this.em.createNativeQuery(query);
        if (dateFrom != null) {
            q.setParameter("dateFrom", dateFrom);
        }
        if (platenumber != null && platenumber != "") {
            q.setParameter("platenumber", platenumber);
        }
        return q;

    }

    @Override
    public List<Map<String, Object>> getAllCarsByFilter(CarmodelDto filter) {

        String query = "select c.platenumber, cs.in_timestamp, cs.in_gate, cs.in_photo_url, cm.model, cm.type ";

        Query q = getQuery(query, filter.getPlateNumber(), filter.getDateFromString());

        List<Object[]> resultList = q.setFirstResult(filter.getPage()).setMaxResults(filter.getElements()).getResultList();
        List<Map<String, Object>> res = new ArrayList<>(resultList.size());
        for (Object[] o : resultList) {
            HashMap<String, Object> resultMap = new HashMap<>();
            if (o[0] != null)
                resultMap.put("platenumber", o[0]);
            if (o[1] != null)
                resultMap.put("in_timestamp", o[1]);
            if (o[2] != null)
                resultMap.put("in_gate", o[2]);
            if (o[3] != null)
                resultMap.put("in_photo_url", o[3]);
            if (o[4] != null)
                resultMap.put("car_model", o[4]);
            if (o[5] != null)
                resultMap.put("dimension", o[5]);
            res.add(resultMap);
        }
        return res;
    }


    @Override
    public Long countCarsByFilter(CarmodelDto filter) {
        String query = "select count(c.id) ";

        Query q = getQuery(query, filter.getPlateNumber(), filter.getDateFromString());

        List<Object[]> res = q.getResultList();
        Long count = Long.valueOf(res.size());
        return count;
    }

//    @Override
//    public List<Map<String, Object>> getAllCarsByFilter22(CarmodelDto filter) {
//
//        String query = "select c.platenumber, cs.in_timestamp, cs.in_gate, cs.in_photo_url, cm.model, cm.type from cars c " +
//                " left join car_model cm on c.model=cm.model " +
//                " left join car_state cs on cs.car_number=c.platenumber " +
//                " where cs.in_timestamp is not null";
//        if (filter.getPlateNumber() != null && filter.getPlateNumber() != "") {
//            query += " and c.platenumber='" + filter.getPlateNumber() + "'";
//        }
//        if (filter.getDateFromString() != null && filter.getDateFromString() != "") {
//            query += " and cs.in_timestamp >= '" + filter.getDateFromString() + "'";
//        }
//        query += " group by c.platenumber  " +
//                " order by cs.id desc";
//
//        Query q = this.em.createNativeQuery(query);
//        List<Object[]> resultList = q.getResultList();
//        List<Map<String, Object>> res = new ArrayList<>(resultList.size());
//        for (Object[] o : resultList) {
//            HashMap<String, Object> resultMap = new HashMap<>();
//            if (o[0] != null)
//                resultMap.put("platenumber", o[0]);
//            if (o[1] != null)
//                resultMap.put("in_timestamp", o[1]);
//            if (o[2] != null)
//                resultMap.put("in_gate", o[2]);
//            if (o[3] != null)
//                resultMap.put("in_photo_url", o[3]);
//            if (o[4] != null)
//                resultMap.put("car_model", o[4]);
//            if (o[5] != null)
//                resultMap.put("dimension", o[5]);
//            res.add(resultMap);
//        }
//        return res;
//    }

}

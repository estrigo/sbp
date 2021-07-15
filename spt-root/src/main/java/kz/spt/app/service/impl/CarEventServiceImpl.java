package kz.spt.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.api.extension.PluginRegister;
import kz.spt.api.plugin.CustomPlugin;
import kz.spt.app.entity.dto.CarEventDto;
import kz.spt.app.service.CarEventService;
import kz.spt.api.service.CarsService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class CarEventServiceImpl implements CarEventService {

    @Autowired
    private CarsService carsService;

    @Lazy
    @Autowired
    private PluginManager pluginManager;

    @Override
    public void saveCarEvent(CarEventDto eventDto) throws Exception {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        PluginWrapper whitelistPlugin = pluginManager.getPlugin("whitelist-plugin");

        if(StringUtils.isNotNullOrEmpty(eventDto.car_number)){
            carsService.createCar(eventDto.car_number);

            //TODO: check camera extraction IN
            if(true){
                if(whitelistPlugin!=null && whitelistPlugin.getPluginState().equals(PluginState.STARTED)){
                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("car_number", eventDto.car_number);
                    node.put("event_time", format.format(eventDto.event_time));

                    List<PluginRegister> pluginRegisters =  pluginManager.getExtensions(PluginRegister.class, whitelistPlugin.getPluginId());
                    if(pluginRegisters.size() > 0){
                        PluginRegister pluginRegister = pluginRegisters.get(0);
                        JsonNode result = pluginRegister.execute(node);

                        boolean whitelistCheckResult = result.get("whitelistCheckResult").booleanValue();
                        if(whitelistCheckResult){
                            System.out.println(eventDto.car_number + " Exist in white list");
                            //TODO open gate
                        } else {
                            System.out.println(eventDto.car_number + " not exist in white list");
                        }
                    }
                }
            } else {
                //TODO check payment plugin or open gate to leave
            }
        }
    }
}

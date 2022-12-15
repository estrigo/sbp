package kz.spt.bookingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.bookingplugin.BookingPlugin;
import kz.spt.bookingplugin.service.BookingService;
import kz.spt.lib.extension.PluginRegister;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

@Slf4j
@Extension
public class CommandExecutor implements PluginRegister {

    private BookingService bookingService;

    @Override
    public JsonNode execute(JsonNode command) {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("bookingResult", false);

        if (command != null && command.has("command")) {
            if ("checkBooking".equals(command.get("command").textValue())) {
                if (command.has("platenumber") && command.get("platenumber").isTextual()) {
                    String position = command.has("position") ? command.get("position").textValue() : "1";
                    String region = command.has("region") ? command.get("region").textValue() : "";
                    String entrance = command.has("entrance") ? command.get("entrance").textValue() : "";
                    Boolean result = false;
                    try {
                        result = getBookingService().checkBookingValid(
                                command.get("platenumber").textValue(), region, position, entrance);
                    } catch (Exception e) {
                        log.error("<< checkBookingValid threw error : {} ", e.getMessage());
                    }
                    node.put("bookingResult", result);
                }
            }
        }

        return node;
    }

    private BookingService getBookingService() {
        if (bookingService == null) {
            bookingService = (BookingService) BookingPlugin.INSTANCE.getApplicationContext().getBean("bookingServiceImpl");
        }
        return bookingService;
    }
}

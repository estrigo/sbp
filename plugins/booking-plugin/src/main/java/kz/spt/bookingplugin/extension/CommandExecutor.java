package kz.spt.bookingplugin.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.bookingplugin.BookingPlugin;
import kz.spt.bookingplugin.service.BookingService;
import kz.spt.lib.extension.PluginRegister;
import lombok.extern.java.Log;
import org.pf4j.Extension;

@Log
@Extension
public class CommandExecutor implements PluginRegister {

    private BookingService bookingService;

    @Override
    public JsonNode execute(JsonNode command) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("bookingResult", false);

        if (command != null && command.has("command")) {
            if ("checkBooking".equals(command.get("command").textValue())) {
                if (command.has("platenumber") && command.get("platenumber").isTextual()) {
                    String position = command.has("position") ? command.get("position").textValue() : "1";
                    Boolean result = getBookingService().checkBookingValid(command.get("platenumber").textValue(), position);
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

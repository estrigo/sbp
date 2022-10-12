package kz.spt.app.service.impl.adminJob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.app.repository.PropertyRepository;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.Property;
import kz.spt.lib.service.AdminService;
import kz.spt.lib.service.PaymentRegistryJob;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRegistryJobImpl implements PaymentRegistryJob {

    private final PropertyRepository propertyRepository;
    private final AdminService adminService;
    private final PluginService pluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final static String CRON_PR = "payment_register";

    public ResponseEntity<?> startPaymentRegistryJob() throws Exception {
        Optional<Property> property = propertyRepository.findFirstByKey(CRON_PR);
        if (property.isPresent()) {
            property.get().setDisabled(false);
            propertyRepository.save(property.get());
            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "startPaymentRegistryJob");
                billingPluginRegister.execute(node);
            }
        }
        return adminService.getBasicResponse();
    }

    public ResponseEntity<?> stopPaymentRegistryJob() throws Exception {
        Optional<Property> property = propertyRepository.findFirstByKey(CRON_PR);
        if (property.isPresent()) {
            property.get().setDisabled(true);
            propertyRepository.save(property.get());
            PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
            if (billingPluginRegister != null) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "stopPaymentRegistryJob");
                billingPluginRegister.execute(node);
            }
        }
        return adminService.getBasicResponse();
    }
}

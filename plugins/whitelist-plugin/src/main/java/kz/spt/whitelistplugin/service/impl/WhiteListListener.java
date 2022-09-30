package kz.spt.whitelistplugin.service.impl;


import kz.spt.lib.model.dto.adminPlace.GenericWhlEvent;

import kz.spt.whitelistplugin.service.RootServicesGetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableAsync
public class WhiteListListener {

    private final RootServicesGetterService rootServicesGetterService;

    @Async
    @EventListener(GenericWhlEvent.class)
      public void whlGroup(GenericWhlEvent<?> whlEvent) {
        rootServicesGetterService.getAdminService().whlProcess(whlEvent);
    }

}

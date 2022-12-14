package kz.spt.app.service.impl;

import kz.spt.app.repository.PropertyRepository;
import kz.spt.lib.model.Property;
import kz.spt.lib.service.PropertyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(noRollbackFor = Exception.class)
public class PropertyServiceImpl implements PropertyService {

    private PropertyRepository propertyRepository;

    public PropertyServiceImpl(PropertyRepository propertyRepository){
        this.propertyRepository = propertyRepository;
    }

    @Override
    public String getValue(String key) {
        Optional<Property> propertyOptional = propertyRepository.findFirstByKey(key);
        if(propertyOptional.isPresent()){
            return propertyOptional.get().getValue();
        }
        return null;
    }

    @Override
    public void setValue(String key, String value) {
        Optional<Property> propertyOptional = propertyRepository.findFirstByKey(key);
        Property property = new Property();
        if(propertyOptional.isPresent()){
            property = propertyOptional.get();
        } else {
            property.setKey(key);
            property.setDisabled(false);
        }
        property.setValue(value);
        propertyRepository.save(property);
    }

    @Override
    public void disable(String key) {
        Optional<Property> propertyOptional = propertyRepository.findFirstByKey(key);
        if(propertyOptional.isPresent()){
            Property property = propertyOptional.get();
            property.setDisabled(true);
            propertyRepository.save(property);
        }
    }
}

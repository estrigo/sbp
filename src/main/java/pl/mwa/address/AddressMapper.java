package pl.mwa.address;

import org.springframework.beans.BeanUtils;

final class AddressMapper {

	private AddressMapper() {}
	
	static AddressDto toDto(Address address) {
		AddressDto addressDto = new AddressDto();
		BeanUtils.copyProperties(address, addressDto);
		return addressDto;
	}
	
	static Address toEntity(CreateAddressDto dto) {
		Address address = new Address();
		BeanUtils.copyProperties(dto, address);
		return address;
	}
	
	
	
}

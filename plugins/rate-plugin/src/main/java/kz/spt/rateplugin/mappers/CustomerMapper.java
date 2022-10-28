//package kz.spt.rateplugin.mappers;
//
//import org.springframework.web.bind.annotation.Mapping;
//
//import org.mapstruct.InjectionStrategy;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//
//@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
//public interface CustomerMapper {
//
//    JustDTO toDTO (Just just);
//    @Mapping(target ="id", ignore = true)
//    Just toModel (JustDTO justDTO);
//
//    @Mapping(target ="id", ignore = true)
//    Just toModel (UpdateJustDTO justDTO);
//
//
//}
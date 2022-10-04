package kz.spt.lib.model.dto.adminPlace;

import kz.spt.lib.model.dto.adminPlace.enums.WhlProcessEnum;

public class GenericWhlEvent<T> {
    private T object;
    protected WhlProcessEnum process;


    public GenericWhlEvent(T object, WhlProcessEnum process) {
        this.object = object;
        this.process = process;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public WhlProcessEnum getProcess() {
        return process;
    }

    public void setProcess(WhlProcessEnum process) {
        this.process = process;
    }
}

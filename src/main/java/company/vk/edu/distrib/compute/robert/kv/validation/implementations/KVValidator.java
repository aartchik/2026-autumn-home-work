package company.vk.edu.distrib.compute.robert.kv.validation.implementations;

import company.vk.edu.distrib.compute.robert.api.models.BadRequestException;
import company.vk.edu.distrib.compute.robert.validation.InputValidator;

public class KVValidator implements InputValidator<byte[]> {
    @Override
    public void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new BadRequestException();
        }
    }

    @Override
    public void validateValue(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
    }
}

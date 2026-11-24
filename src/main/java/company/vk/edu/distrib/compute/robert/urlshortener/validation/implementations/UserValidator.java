package company.vk.edu.distrib.compute.robert.urlshortener.validation.implementations;

import company.vk.edu.distrib.compute.robert.validation.InputValidator;

public class UserValidator implements InputValidator<String> {
    @Override 
    public void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    @Override 
    public void validateValue(String value) {
        if (value == null || value.isEmpty()) { 
            throw new IllegalArgumentException();
        }
    }   
}

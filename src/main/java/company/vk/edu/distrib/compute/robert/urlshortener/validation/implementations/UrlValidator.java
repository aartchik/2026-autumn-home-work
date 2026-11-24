package company.vk.edu.distrib.compute.robert.urlshortener.validation.implementations;

import company.vk.edu.distrib.compute.robert.validation.InputValidator;

public class UrlValidator implements InputValidator<String> {
    @Override 
    public void validateKey(String key) {
        if (key == null
            || key.length() != 10
            || key.isEmpty()
            || key.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))
        ) { 
            throw new IllegalArgumentException();
        }
    }

    @Override 
    public void validateValue(String value) {
        if (value == null
                || (
                    !value.startsWith("http://")
                    && !value.startsWith("https://")
                )
        ) {
            throw new IllegalArgumentException();
        }
    }   
}

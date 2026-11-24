package company.vk.edu.distrib.compute.robert.validation;

public interface InputValidator<T> {
    void validateKey(String key);
    
    void validateValue(T value);
}

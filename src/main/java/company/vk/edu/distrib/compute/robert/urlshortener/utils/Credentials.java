package company.vk.edu.distrib.compute.robert.urlshortener.utils;

public record Credentials(
    String user,
    String password
) {
    private static final int CREDENTIALS_PARTS_COUNT = 2;

    public static Credentials from(String str) {
        
        if (str == null) {
            throw new IllegalArgumentException();
        }

        String[] list = str.split(":", 2);
        if (list.length != CREDENTIALS_PARTS_COUNT) {
            throw new IllegalArgumentException();
        }

        return new Credentials(list[0], list[1]);
    }
}

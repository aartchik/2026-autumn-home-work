package company.vk.edu.distrib.compute.aartchik.urlshortener;

record HttpResponseData(int status, String body) {
    HttpResponseData(int status) {
        this(status, "");
    }
}

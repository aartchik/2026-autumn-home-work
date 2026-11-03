package company.vk.edu.distrib.compute.aartchik.kv;

record KvResponse(int status, byte[] body) {
    KvResponse(int status) {
        this(status, new byte[0]);
    }
}

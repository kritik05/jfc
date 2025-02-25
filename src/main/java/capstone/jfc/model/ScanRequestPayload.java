package capstone.jfc.model;
import java.util.Objects;

public class ScanRequestPayload {
    private Integer tenantId;         // for multi-tenant
    private String types; // or List<ScanType> if you have an enum

    public ScanRequestPayload() {}

    public ScanRequestPayload(Integer tenantId, String types) {
        this.tenantId = tenantId;
        this.types = types;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public String getTypes() {
        return types;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScanRequestPayload that = (ScanRequestPayload) o;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, types);
    }

    @Override
    public String toString() {
        return "ScanRequestPayload{" +
                "tenantId=" + tenantId +
                ", types='" + types + '\'' +
                '}';
    }
}

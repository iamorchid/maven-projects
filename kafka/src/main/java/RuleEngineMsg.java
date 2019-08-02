import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;

/**
 * Created by changyi.yuan on 2018/8/24.
 */
@Data
public class RuleEngineMsg
{
    private final static String STATIC_NAME = "iot-hub-parser";
    private final static String STATIC_ID = "-10000";

    private String clientId = STATIC_ID;
    private String thingName = STATIC_NAME;
    private String licenseId = STATIC_ID;
    private String hubId = STATIC_ID;
    private String userId = STATIC_ID;
    private int strategyId = Integer.parseInt(STATIC_ID);

    private String topicName;
    private String customerId;
    private Object payload;

    private String orgId;
    private String modelId;
    private String modelIdPath;

    public RuleEngineMsg(String topicName, String customerId, JsonObject payload, String orgId, String modelId, String modelIdPath) {
        this.topicName = topicName;
        this.customerId = customerId;
        this.payload = payload;
        this.orgId = orgId;
        this.modelId = modelId;
        this.modelIdPath = modelIdPath;
    }

    public RuleEngineMsg(String topicName, String customerId, JsonArray payload, String orgId, String modelId, String modelIdPath) {
        this.topicName = topicName;
        this.customerId = customerId;
        this.payload = payload;
        this.orgId = orgId;
        this.modelId = modelId;
        this.modelIdPath = modelIdPath;
    }

    public RuleEngineMsg(String topicName, String customerId, String payload, String orgId, String modelId, String modelIdPath) {
        this.topicName = topicName;
        this.customerId = customerId;
        this.payload = payload;
        this.orgId = orgId;
        this.modelId = modelId;
        this.modelIdPath = modelIdPath;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Object getPayload() {
        return payload;
    }

    public String getOrgId()
    {
        return orgId;
    }

    public String getModelId()
    {
        return modelId;
    }

    public String getModelIdPath() {
        return modelIdPath;
    }
}

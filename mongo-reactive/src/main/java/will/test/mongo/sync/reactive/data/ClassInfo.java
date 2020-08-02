package will.test.mongo.sync.reactive.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.Date;

/**
 * See {@link BasicMongoPersistentProperty} for class field annotation
 */
@Data
public class ClassInfo {
    @Id
    private String classId;

    @Field(value = "name", targetType = FieldType.STRING)
    private String className;

//    /**
//     * TODO 为何下面的转换会失败？及时已经定义了String2DateConverterFactory
//     */
//    @Field(value = "pubDate", targetType = FieldType.DATE_TIME)
//    private Date publishDate;
}

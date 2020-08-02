package will.test.mongo.sync.reactive.components;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class String2DateConverterFactory implements ConverterFactory<String, Date> {

    @Override
    public <T extends Date> Converter<String, T> getConverter(Class<T> targetType) {
        return new String2DateConverter<T>();
    }


    private static class String2DateConverter<T extends Date> implements Converter<String, T> {
        @Override
        public T convert(String s) {
            try {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                return (T) format.parse(s);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
